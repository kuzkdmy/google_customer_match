package com.test.gcm.routees

import cats.implicits.catsSyntaxEitherId
import com.google.ads.googleads.v13.resources.UserList
import com.test.gcm.domain._
import com.test.gcm.routees.UserListRoutesApi.{createUserListE, getUserListByNameE, CreateUserListRequest, UserListResponse}
import com.test.gcm.service.GCMUserListService
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, ZIO, ZLayer}

object UserListRoutesImpl {
  type Env = UserListRoutesService
  def endpoints[R <: Env]: List[ZServerEndpoint[R, ZioStreams]] = List(
    getUserListByNameE.serverLogic(i => ZIO.serviceWithZIO[Env](_.getUserListByName(i))): ZServerEndpoint[R, ZioStreams],
    createUserListE.serverLogic(i => ZIO.serviceWithZIO[Env](_.createUserList(i))): ZServerEndpoint[R, ZioStreams]
  )
}

trait UserListRoutesService {
  def getUserListByName(cmd: (ConnectionId, UserListName)): Task[Either[Unit, UserListResponse]]
  def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]]
}

case class UserListRoutesServiceImpl(svc: GCMUserListService) extends UserListRoutesService {
  override def getUserListByName(cmd: (ConnectionId, UserListName)): Task[Either[Unit, UserListResponse]] = {
    val (connectionId, userListName) = cmd
    for {
      userList <- svc.getUserListByName(connectionId, userListName)
      res <- userList match {
               case Some(v) => ZIO.succeed(toUserListResponse(v))
               case None    => ZIO.fail(NotFoundError(connectionId.value, userListName.value))
             }
    } yield res.asRight
  }
  override def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]] = {
    svc
      .getOrCreateUserList(cmd.connectionId, cmd.listName)
      .map(v => toUserListResponse(v).asRight)
  }

  private def toUserListResponse(v: UserList): UserListResponse = {
    UserListResponse(
      UserListId(v.getId),
      UserListName(v.getName),
      UserListResourceName(v.getResourceName),
      UserListDescription(v.getDescription),
      UserListMatchRatePercentage(v.getMatchRatePercentage)
    )
  }
}

object UserListRoutesServiceImpl {
  lazy val layer: ZLayer[GCMUserListService, Nothing, UserListRoutesService] =
    ZLayer.fromFunction(UserListRoutesServiceImpl.apply _)
}
