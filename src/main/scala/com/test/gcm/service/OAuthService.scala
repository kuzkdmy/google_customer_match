package com.test.gcm.service

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import com.test.gcm.domain._
import com.test.gcm.repository.GCMConnectionsStore
import zio.{&, Task, URLayer, ZIO, ZLayer}

import java.time.LocalDateTime
import java.util.Collections

/** https://developers.google.com/google-ads/api/docs/oauth/cloud-project
  */
trait OAuthService {
  def oauthAttempt(customerId: CustomerId, developerToken: CustomerDeveloperToken): Task[OAuthConnectionAttempt]
  def completeOauthAttempt(id: ConnectionId, code: CompleteOauthCode): Task[OAuthConnection]
}

case class OAuthServiceImpl(repo: GCMConnectionsStore, conf: GoogleServerApiCredsConfig) extends OAuthService {
  private val redirectUri = "urn:ietf:wg:oauth:2.0:oob"

  override def oauthAttempt(customerId: CustomerId, developerToken: CustomerDeveloperToken): Task[OAuthConnectionAttempt] = {
    for {
      id          <- zio.Random.nextUUID.map(v => ConnectionId(v.toString))
      _           <- ZIO.logInfo(s"init oauth attempt, id:$id")
      flow        <- authCodeFlow()
      redirectUrl <- ZIO.attempt(OAuthRedirectURL(flow.newAuthorizationUrl().setRedirectUri(redirectUri).build()))
      _           <- ZIO.logInfo(s"init oauth attempt, id:$id success, redirect url: $redirectUrl")
      connection  <- repo.save(OAuthConnectionAttempt(id, customerId, developerToken, redirectUrl, LocalDateTime.now()))
      _           <- ZIO.logInfo(s"init oauth attempt saved in db success, id:$id")
    } yield connection
  }

  override def completeOauthAttempt(id: ConnectionId, code: CompleteOauthCode): Task[OAuthConnection] = {
    for {
      _            <- ZIO.logInfo("complete oauth attempt")
      oauthAttempt <- getOauthAttempt(id)
      auth         <- authCodeFlow()
      tokenResp    <- ZIO.attempt(auth.newTokenRequest(code.value).setRedirectUri(redirectUri).execute())
      _ <- ZIO.logInfo(s"""complete oauth success, id: $id,
           |token expire in seconds: ${tokenResp.getExpiresInSeconds},
           |access token: `${tokenResp.getAccessToken}`,
           |refresh token: `${tokenResp.getRefreshToken}`
           |""".replaceAll("\n", " ").stripMargin)
      accessToken  = OAuthAccessToken(tokenResp.getAccessToken)
      refreshToken = OAuthRefreshToken(tokenResp.getRefreshToken)
      res <- repo.save(OAuthConnection.completeOauthState(oauthAttempt, code, accessToken, refreshToken, tokenResp.getExpiresInSeconds))
      _   <- ZIO.logInfo(s"save oauth into db, id: $id")
    } yield res
  }

  private def getOauthAttempt(id: ConnectionId): Task[OAuthConnectionAttempt] = {
    for {
      connection <- repo.get(id).someOrFail(new Exception(s"connection not found, id: $id"))
      res <- connection match {
               case v: OAuthConnectionAttempt => ZIO.succeed(v)
               case _: OAuthConnection        => ZIO.fail(new Exception(s"connection id: $id already authorized"))
             }
    } yield res
  }

  private def authCodeFlow(): Task[GoogleAuthorizationCodeFlow] = ZIO.attempt(
    new GoogleAuthorizationCodeFlow.Builder(
      new NetHttpTransport(),
      GsonFactory.getDefaultInstance,
      conf.clientId,
      conf.clientSecret,
      Collections.singletonList("https://www.googleapis.com/auth/adwords")
    )
      .setAccessType("offline")
      .setDataStoreFactory(new MemoryDataStoreFactory())
      .build()
  )

}

object OAuthServiceImpl {
  lazy val layer: URLayer[GCMConnectionsStore & GoogleServerApiCredsConfig, OAuthService] =
    ZLayer.fromFunction(OAuthServiceImpl.apply _)
}
