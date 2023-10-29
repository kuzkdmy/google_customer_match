package com.test.gcm.routees

import cats.implicits.catsSyntaxEitherId
import com.test.gcm.routees.OAuthRoutesApi._
import com.test.gcm.service.OAuthService
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, ZIO, ZLayer}

object OAuthRoutesImpl {
  type Env = OAuthRoutesService

  def endpoints[R <: Env]: List[ZServerEndpoint[R, ZioStreams]] = List(
    initOAuthE.serverLogic(_ => ZIO.serviceWithZIO[Env](_.initOAuthAttempt())): ZServerEndpoint[R, ZioStreams],
    completeOAuthE.serverLogic(i => ZIO.serviceWithZIO[Env](_.completeOAuthAttempt(i))): ZServerEndpoint[R, ZioStreams]
  )

}

trait OAuthRoutesService {
  def initOAuthAttempt(): Task[Either[Unit, InitOAuthResponse]]
  def completeOAuthAttempt(req: CompleteOAuthRequest): Task[Either[Unit, CompleteOAuthResponse]]
}

case class OAuthRoutesServiceImpl(svc: OAuthService) extends OAuthRoutesService {

  override def initOAuthAttempt(): Task[Either[Unit, InitOAuthResponse]] = {
    for {
      e <- svc.oauthAttempt()
      res = InitOAuthResponse(id = e.id, redirectUrl = e.redirectUrl)
    } yield res.asRight

  }

  override def completeOAuthAttempt(req: CompleteOAuthRequest): Task[Either[Unit, CompleteOAuthResponse]] = {
    for {
      e <- svc.completeOauthAttempt(req.id, req.code)
      res = CompleteOAuthResponse(
              id                                  = req.id,
              tokenExpiresInSeconds               = e.getExpiresInSeconds,
              asNoStorageOnServerSideAccessToken  = e.getAccessToken,
              asNoStorageOnServerSideRefreshToken = e.getRefreshToken
            )
    } yield res.asRight
  }

}

object OAuthRoutesServiceImpl {

  val layer: ZLayer[OAuthService, Nothing, OAuthRoutesService] =
    ZLayer.fromFunction(OAuthRoutesServiceImpl.apply _)

}
