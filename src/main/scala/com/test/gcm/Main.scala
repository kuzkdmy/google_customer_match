package com.test.gcm

import com.test.gcm.config.AppConfig
import com.test.gcm.repository.GCMConnectionsStoreImpl
import com.test.gcm.routees.{OAuthRoutesImpl, OAuthRoutesServiceImpl, UserListRoutesImpl, UserListRoutesServiceImpl}
import com.test.gcm.service._
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.Server
import zio.logging.consoleLogger
import zio.{Scope, ZIO, ZIOAppArgs, ZLayer}

object Main extends zio.ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    zio.Runtime.removeDefaultLoggers >>> consoleLogger()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    type Env = OAuthRoutesImpl.Env with UserListRoutesImpl.Env
    val routes: zio.http.HttpApp[Env] =
      ZioHttpInterpreter().toHttp(OAuthRoutesImpl.endpoints[Env] ++ UserListRoutesImpl.endpoints[Env])
    ZIO.logInfo("Starting server with default port : 8080") *>
      Server
        .serve(routes)
        .provide(
          AppConfig.layer,
          OAuthServiceImpl.layer,
          OAuthRoutesServiceImpl.layer,
          Server.default,
          GCMClientsImpl.layer,
          GCMJobServiceImpl.layer,
          GCMUserListServiceImpl.layer,
          UserListRoutesServiceImpl.layer,
          GCMConnectionsStoreImpl.layer,
          ApiConverterImpl.layer
        )
  }

}
