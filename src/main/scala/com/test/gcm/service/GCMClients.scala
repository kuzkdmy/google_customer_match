package com.test.gcm.service

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v13.services.{GoogleAdsServiceClient, OfflineUserDataJobServiceClient, UserListServiceClient}
import com.google.auth.oauth2.UserCredentials
import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import zio.{RIO, Scope, Task, URLayer, ZIO, ZLayer}

trait GCMClients {
  def googleAdsClient(): Task[GoogleAdsClient]
  def userListServiceClient(): RIO[Scope, UserListServiceClient]
  def offlineUserDataJobServiceClient(): RIO[Scope, OfflineUserDataJobServiceClient]
  def googleAdsServiceClient(): RIO[Scope, GoogleAdsServiceClient]
}

case class GCMClientsImpl(conf: GoogleServerApiCredsConfig) extends GCMClients {

  override def googleAdsClient(): Task[GoogleAdsClient] = {
    ZIO.attempt(
      GoogleAdsClient
        .newBuilder()
        //    .setDeveloperToken(secretDeveloperToken)
        .setCredentials(
          UserCredentials
            .newBuilder()
            .setClientId(conf.clientId)
            .setClientSecret(conf.clientSecret)
            //        .setRefreshToken(secretRefreshToken)
            .build()
        )
        .build()
    )
  }

  override def userListServiceClient(): RIO[Scope, UserListServiceClient] = {
    googleAdsClient().flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createUserListServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def offlineUserDataJobServiceClient(): RIO[Scope, OfflineUserDataJobServiceClient] = {
    googleAdsClient().flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createOfflineUserDataJobServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

  override def googleAdsServiceClient(): RIO[Scope, GoogleAdsServiceClient] = {
    googleAdsClient().flatMap(c => ZIO.acquireRelease(ZIO.attempt(c.getLatestVersion.createGoogleAdsServiceClient()))(c => ZIO.attempt(c.close()).ignore))
  }

}

object GCMClientsImpl {

  lazy val layer: URLayer[GoogleServerApiCredsConfig, GCMClients] =
    ZLayer.fromFunction(GCMClientsImpl.apply _)

}
