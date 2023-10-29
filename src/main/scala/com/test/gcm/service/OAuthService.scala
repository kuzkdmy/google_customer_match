package com.test.gcm.service

import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleTokenResponse}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import com.test.gcm.domain.OAuthAttempt
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.Collections

/** https://developers.google.com/google-ads/api/docs/oauth/cloud-project
  */
trait OAuthService {
  def oauthAttempt(): Task[OAuthAttempt]
  def completeOauthAttempt(id: String, code: String): Task[GoogleTokenResponse]
}

case class OAuthServiceImpl(conf: GoogleServerApiCredsConfig) extends OAuthService {
  private val redirectUri = "urn:ietf:wg:oauth:2.0:oob"

  override def oauthAttempt(): Task[OAuthAttempt] = {
    for {
      id          <- zio.Random.nextUUID.map(_.toString)
      _           <- ZIO.logInfo(s"init oauth attempt, id:$id")
      auth        <- authCodeFlow()
      redirectUrl <- ZIO.attempt(auth.newAuthorizationUrl().setRedirectUri(redirectUri).build())
      _           <- ZIO.logInfo(s"init oauth attempt, id:$id success, redirect url: $redirectUrl")
    } yield OAuthAttempt(id, redirectUrl)
  }

  override def completeOauthAttempt(id: String, code: String): Task[GoogleTokenResponse] = {
    for {
      _         <- ZIO.logInfo("complete oauth attempt")
      auth      <- authCodeFlow()
      tokenResp <- ZIO.attempt(auth.newTokenRequest(code).setRedirectUri(redirectUri).execute())
      _ <- ZIO.logInfo(s"""complete oauth success,
           |token expire in seconds: ${tokenResp.getExpiresInSeconds},
           |access token: `${tokenResp.getAccessToken}`,
           |refresh token: `${tokenResp.getRefreshToken}`
           |""".replaceAll("\n", " ").stripMargin)
    } yield tokenResp

  }

  private def authCodeFlow() = ZIO.attempt(
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

  lazy val layer: URLayer[GoogleServerApiCredsConfig, OAuthService] =
    ZLayer.fromFunction(OAuthServiceImpl.apply _)

}
