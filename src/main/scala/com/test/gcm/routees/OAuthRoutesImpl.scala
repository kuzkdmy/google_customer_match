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
    initOAuthE.serverLogic(i => ZIO.serviceWithZIO[Env](_.initOAuthAttempt(i))): ZServerEndpoint[R, ZioStreams],
    completeOAuthE.serverLogic(i => ZIO.serviceWithZIO[Env](_.completeOAuthAttempt(i))): ZServerEndpoint[R, ZioStreams]
  )
}

trait OAuthRoutesService {
  def initOAuthAttempt(req: InitOAuthRequest): Task[Either[Unit, InitOAuthResponse]]
  def completeOAuthAttempt(req: CompleteOAuthRequest): Task[Either[Unit, CompleteOAuthResponse]]
}

case class OAuthRoutesServiceImpl(svc: OAuthService) extends OAuthRoutesService {
  override def initOAuthAttempt(req: InitOAuthRequest): Task[Either[Unit, InitOAuthResponse]] = {
    svc
      .oauthAttempt(req.customerId, req.developerToken)
      .map(e => InitOAuthResponse(e.id, e.redirectUrl).asRight)
  }

  override def completeOAuthAttempt(req: CompleteOAuthRequest): Task[Either[Unit, CompleteOAuthResponse]] = {
    svc
      .completeOauthAttempt(req.id, req.code)
      .map(e => CompleteOAuthResponse(req.id, e.tokenExpiresAt).asRight)
  }
}

object OAuthRoutesServiceImpl {
  lazy val layer: ZLayer[OAuthService, Nothing, OAuthRoutesService] =
    ZLayer.fromFunction(OAuthRoutesServiceImpl.apply _)
}
