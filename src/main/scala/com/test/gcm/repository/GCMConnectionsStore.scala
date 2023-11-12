package com.test.gcm.repository

import com.test.gcm.domain._
import sttp.model.StatusCode
import zio.{Ref, Task, TaskLayer, ZIO, ZLayer}

trait GCMConnectionsStore {
  def get(id: ConnectionId): Task[Option[IsOAuthConnection]]
  def save[T <: IsOAuthConnection](connection: T): Task[T]
  def getOauthConnection(id: ConnectionId): Task[OAuthConnection]
}

class GCMConnectionsStoreImpl(inMemoryStore: Ref[Map[ConnectionId, IsOAuthConnection]]) extends GCMConnectionsStore {
  override def get(id: ConnectionId): Task[Option[IsOAuthConnection]] =
    inMemoryStore.get.map(_.get(id))

  override def save[T <: IsOAuthConnection](connection: T): Task[T] =
    inMemoryStore.update(_.updated(connection.id, connection)).as(connection)

  override def getOauthConnection(id: ConnectionId): Task[OAuthConnection] = {
    for {
      connOpt <- get(id)
      conn <- connOpt match {
                case None                            => ZIO.fail(NotFoundError(id.value, "Connection"))
                case Some(_: OAuthConnectionAttempt) => ZIO.fail(CustomError(s"Connection ${id.value} is not authorized", StatusCode.BadRequest))
                case Some(v: OAuthConnection)        => ZIO.succeed(v)
              }
    } yield conn
  }
}

object GCMConnectionsStoreImpl {
  lazy val layer: TaskLayer[GCMConnectionsStore] = {
    ZLayer.fromZIO(Ref.make(Map.empty[ConnectionId, IsOAuthConnection]).map(new GCMConnectionsStoreImpl(_)))
  }
}
