package com.test.gcm

import com.test.gcm.config.AppConfig
import com.test.gcm.repository.GCMTokenStoreImpl
import com.test.gcm.routees.{OAuthRoutesImpl, OAuthRoutesServiceImpl, UserListRoutesImpl, UserListRoutesServiceImpl}
import com.test.gcm.service.{GCMClientsImpl, GCMJobServiceImpl, GCMUserListServiceImpl, OAuthServiceImpl}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.{Response, Server, Status}
import zio.logging.consoleLogger
import zio.{Scope, ZIO, ZIOAppArgs, ZLayer}

object Main extends zio.ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    zio.Runtime.removeDefaultLoggers >>> consoleLogger()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    type Env = OAuthRoutesImpl.Env with UserListRoutesImpl.Env
    val routes: zio.http.App[Env] =
      ZioHttpInterpreter()
        .toHttp(OAuthRoutesImpl.endpoints[Env] ++ UserListRoutesImpl.endpoints[Env])
        .mapErrorZIO { e =>
          // todo, need to check, this not works now
          println("!!!!!!!")
          println(s"ERROR, ${e.getMessage}")
          println("!!!!!!!")
          ZIO
            .logError(s"[internal server error] ${e.getMessage}")
            .as(Response(status = Status.InternalServerError))
        }
        .asInstanceOf[zio.http.App[Env]]
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
          GCMTokenStoreImpl.layer
        )
  }

}
