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
    initOAuthAttemptE.serverLogic(_ =>
      ZIO.serviceWithZIO[Env](_.initOAuthAttempt())): ZServerEndpoint[R, ZioStreams],
    completeOAuthAttemptE.serverLogic(i =>
      ZIO.serviceWithZIO[Env](_.completeOAuthAttempt(i))): ZServerEndpoint[R, ZioStreams])

}

trait OAuthRoutesService {
  def initOAuthAttempt(): Task[Either[Unit, InitOAuthAttemptResponse]]

  def completeOAuthAttempt(
      req: CompleteOAuthAttemptRequest): Task[Either[Unit, CompleteOAuthAttemptResponse]]

}

case class OAuthRoutesServiceImpl(svc: OAuthService) extends OAuthRoutesService {

  override def initOAuthAttempt(): Task[Either[Unit, InitOAuthAttemptResponse]] = {
    for {
      e <- svc.oauthAttempt()
      res = InitOAuthAttemptResponse(id = e.id, redirectUrl = e.redirectUrl)
    } yield res.asRight

  }

  override def completeOAuthAttempt(
      req: CompleteOAuthAttemptRequest): Task[Either[Unit, CompleteOAuthAttemptResponse]] = {
    for {
      e <- svc.completeOauthAttempt(req.code)
      res = CompleteOAuthAttemptResponse(id = req.id, status = "success")
    } yield res.asRight
  }

}

object OAuthRoutesServiceImpl {

  val layer: ZLayer[OAuthService, Nothing, OAuthRoutesService] =
    ZLayer.fromFunction(OAuthRoutesServiceImpl.apply _)

}
