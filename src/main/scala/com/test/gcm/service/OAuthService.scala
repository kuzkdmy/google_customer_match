package com.test.gcm.service

import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleTokenResponse}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import com.test.gcm.domain.OAuthAttempt
import zio.{Task, URLayer, ZIO, ZLayer}

import java.util.Collections

/**
 * https://developers.google.com/google-ads/api/docs/oauth/cloud-project
 */
trait OAuthService {
  def oauthAttempt(): Task[OAuthAttempt]
  def completeOauthAttempt(code: String): Task[GoogleTokenResponse]
}

case class OAuthServiceImpl(conf: GoogleServerApiCredsConfig) extends OAuthService {
  private val redirectUri = "urn:ietf:wg:oauth:2.0:oob"

  override def oauthAttempt(): Task[OAuthAttempt] = {
    for {
      _ <- ZIO.logInfo("init oauth attempt")
      auth <- authCodeFlow()
      redirectUrl <- ZIO.attempt(auth.newAuthorizationUrl().setRedirectUri(redirectUri).build())
      _ <- ZIO.logInfo(s"init oauth attempt success, ${redirectUrl}")
    } yield OAuthAttempt("123", redirectUrl, "pending")
  }


//
  //    // Print the authorization URL and have the user visit it to authorize your application
  //    println("Authorization URL: " + authorizationUrl)
  //
  //    // After the user authorizes your application, they will be redirected to your specified redirect URI
  //    // Parse the authorization code from the query parameter in the redirect URI
  //    val authorizationCode = "AUTHORIZATION_CODE_FROM_REDIRECT_URI"
  //
  //    // Exchange the authorization code for access and refresh tokens
  //    val tokenResponse = authorizationCodeFlow
  //      .newTokenRequest(authorizationCode)
  //      .setRedirectUri(redirectUri)
  //      .execute()
  //
  //    // Access token
  //    val accessToken = tokenResponse.getAccessToken
  //
  //    // Refresh token
  //    val refreshToken = tokenResponse.getRefreshToken
  //
  //    println("Access Token: " + accessToken)
  //    println("Refresh Token: " + refreshToken)

  override def completeOauthAttempt(code: String): Task[GoogleTokenResponse] = {
    for {
      _ <- ZIO.logInfo("complete oauth attempt")
      auth <- authCodeFlow()
      tokenResp <- ZIO.attempt(auth.newTokenRequest(code).setRedirectUri(redirectUri).execute())
      _ <- ZIO.logInfo(s"complete oauth attempt success, token expire in seconds: ${tokenResp.getExpiresInSeconds}")
    } yield tokenResp

  }

  private def authCodeFlow() = ZIO.attempt(
    new GoogleAuthorizationCodeFlow.Builder(
      new NetHttpTransport(),
      GsonFactory.getDefaultInstance,
      conf.clientId,
      conf.clientSecret,
      Collections.singletonList("https://www.googleapis.com/auth/adwords"))
      .setAccessType("offline")
      .setDataStoreFactory(new MemoryDataStoreFactory())
      .build())

}

object OAuthServiceImpl {

  lazy val layer: URLayer[GoogleServerApiCredsConfig, OAuthService] =
    ZLayer.fromFunction(OAuthServiceImpl.apply _)

}
