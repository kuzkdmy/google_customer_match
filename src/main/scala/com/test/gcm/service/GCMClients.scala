package com.test.gcm.service

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v13.services.{GoogleAdsServiceClient, OfflineUserDataJobServiceClient, UserListServiceClient}
import com.google.auth.oauth2.{AccessToken, UserCredentials}
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import com.test.gcm.domain.OAuthConnection
import zio.{RIO, Scope, Task, URLayer, ZIO, ZLayer}

trait GCMClients {
  def googleAdsClient(connection: OAuthConnection): Task[GoogleAdsClient]
  def userListServiceClient(connection: OAuthConnection): RIO[Scope, UserListServiceClient]
  def offlineUserDataJobServiceClient(connection: OAuthConnection): RIO[Scope, OfflineUserDataJobServiceClient]
  def googleAdsServiceClient(connection: OAuthConnection): RIO[Scope, GoogleAdsServiceClient]
}

case class GCMClientsImpl(conf: GoogleServerApiCredsConfig) extends GCMClients {

  override def googleAdsClient(connection: OAuthConnection): Task[GoogleAdsClient] = {
    ZIO.attempt(
      GoogleAdsClient
        .newBuilder()
        .setDeveloperToken(connection.customerDevToken.value)
        .setLoginCustomerId(connection.customerId.value)
        .setCredentials(
          UserCredentials
            .newBuilder()
            .setClientId(conf.clientId)
            .setClientSecret(conf.clientSecret)
            .setAccessToken(
              AccessToken
                .newBuilder()
                .setTokenValue(connection.accessToken.value)
                .build()
            )
            //        .setRefreshToken(secretRefreshToken)
            .build()
        )
        .build()
    )
  }

  override def userListServiceClient(connection: OAuthConnection): RIO[Scope, UserListServiceClient] = {
    googleAdsClient(connection).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createUserListServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def offlineUserDataJobServiceClient(connection: OAuthConnection): RIO[Scope, OfflineUserDataJobServiceClient] = {
    googleAdsClient(connection).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createOfflineUserDataJobServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def googleAdsServiceClient(connection: OAuthConnection): RIO[Scope, GoogleAdsServiceClient] = {
    googleAdsClient(connection).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createGoogleAdsServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }
}

object GCMClientsImpl {
  lazy val layer: URLayer[GoogleServerApiCredsConfig, GCMClients] =
    ZLayer.fromFunction(GCMClientsImpl.apply _)
}
