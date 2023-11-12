package com.test.gcm.repository

import com.test.gcm.domain.{ConnectionId, IsOAuthConnection}
import zio.{Ref, Task, TaskLayer, ZLayer}

trait GCMConnectionsStore {
  def get(id: ConnectionId): Task[Option[IsOAuthConnection]]
  def save[T <: IsOAuthConnection](connection: T): Task[T]
}

class GCMTokenStoreImpl(inMemoryStore: Ref[Map[ConnectionId, IsOAuthConnection]]) extends GCMConnectionsStore {
  override def get(id: ConnectionId): Task[Option[IsOAuthConnection]] =
    inMemoryStore.get.map(_.get(id))

  override def save[T <: IsOAuthConnection](connection: T): Task[T] =
    inMemoryStore.update(_.updated(connection.id, connection)).as(connection)
}

object GCMTokenStoreImpl {
  lazy val layer: TaskLayer[GCMConnectionsStore] = {
    ZLayer.fromZIO(Ref.make(Map.empty[ConnectionId, IsOAuthConnection]).map(new GCMTokenStoreImpl(_)))
  }
}
