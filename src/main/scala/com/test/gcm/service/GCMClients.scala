package com.test.gcm.service

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v13.services.{GoogleAdsServiceClient, OfflineUserDataJobServiceClient, UserListServiceClient}
import com.google.auth.oauth2.{AccessToken, UserCredentials}
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import com.test.gcm.domain.CustomerOAuth
import zio.{RIO, Scope, Task, URLayer, ZIO, ZLayer}

trait GCMClients {
  def googleAdsClient(customerOAuth: CustomerOAuth): Task[GoogleAdsClient]
  def userListServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, UserListServiceClient]
  def offlineUserDataJobServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, OfflineUserDataJobServiceClient]
  def googleAdsServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, GoogleAdsServiceClient]
}

case class GCMClientsImpl(conf: GoogleServerApiCredsConfig) extends GCMClients {

  override def googleAdsClient(customerOAuth: CustomerOAuth): Task[GoogleAdsClient] = {
    ZIO.attempt(
      GoogleAdsClient
        .newBuilder()
        .setDeveloperToken(customerOAuth.developerToken.value)
        .setLoginCustomerId(customerOAuth.customerId.value)
        .setCredentials(
          UserCredentials
            .newBuilder()
            .setClientId(conf.clientId)
            .setClientSecret(conf.clientSecret)
            .setAccessToken(
              AccessToken
                .newBuilder()
                .setTokenValue(customerOAuth.accessToken.value)
                .build()
            )
            //        .setRefreshToken(secretRefreshToken)
            .build()
        )
        .build()
    )
  }

  override def userListServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, UserListServiceClient] = {
    googleAdsClient(customerOAuth).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createUserListServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def offlineUserDataJobServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, OfflineUserDataJobServiceClient] = {
    googleAdsClient(customerOAuth).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createOfflineUserDataJobServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def googleAdsServiceClient(customerOAuth: CustomerOAuth): RIO[Scope, GoogleAdsServiceClient] = {
    googleAdsClient(customerOAuth).flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createGoogleAdsServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

}

object GCMClientsImpl {

  lazy val layer: URLayer[GoogleServerApiCredsConfig, GCMClients] =
    ZLayer.fromFunction(GCMClientsImpl.apply _)

}
