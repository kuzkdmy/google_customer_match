package com.test.gcm.routees

import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import com.google.ads.googleads.v13.resources.UserList
import com.google.ads.googleads.v13.services.OfflineUserDataJobOperation
import com.test.gcm.domain._
import com.test.gcm.routees.UserListRoutesApi._
import com.test.gcm.service.{ApiConverter, GCMJobService, GCMUserListService}
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.ZServerEndpoint
import zio.interop.catz._
import zio.{&, Task, UIO, URLayer, ZIO, ZLayer}

object UserListRoutesImpl {
  type Env = UserListRoutesService
  def endpoints[R <: Env]: List[ZServerEndpoint[R, ZioStreams]] = List(
    getUserListByNameE.serverLogic(i => ZIO.serviceWithZIO[Env](_.getUserListByName(i))): ZServerEndpoint[R, ZioStreams],
    createUserListE.serverLogic(i => ZIO.serviceWithZIO[Env](_.createUserList(i))): ZServerEndpoint[R, ZioStreams],
    operateListMembersE.serverLogic(i => ZIO.serviceWithZIO[Env](_.operateListMembers(i))): ZServerEndpoint[R, ZioStreams]
  )
}

trait UserListRoutesService {
  def getUserListByName(cmd: (ConnectionId, UserListName)): Task[Either[Unit, UserListResponse]]
  def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]]
  def operateListMembers(cmd: OperateListMembersRequest): Task[Either[Unit, OperateListMembersResponse]]
}

case class UserListRoutesServiceImpl(
    listSvc: GCMUserListService,
    jobSvc: GCMJobService,
    converter: ApiConverter
) extends UserListRoutesService {
  override def getUserListByName(cmd: (ConnectionId, UserListName)): Task[Either[Unit, UserListResponse]] = {
    val (connectionId, userListName) = cmd
    for {
      userList <- listSvc.getUserListByName(connectionId, userListName)
      res <- userList match {
               case Some(v) => ZIO.succeed(toUserListResponse(v))
               case None    => ZIO.fail(NotFoundError(connectionId.value, userListName.value))
             }
    } yield res.asRight
  }
  override def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]] = {
    listSvc
      .getOrCreateUserList(cmd.connectionId, cmd.listName)
      .map(v => toUserListResponse(v).asRight)
  }

  override def operateListMembers(cmd: OperateListMembersRequest): Task[Either[Unit, OperateListMembersResponse]] = {
    for {
      listOpt <- listSvc.getUserListById(cmd.connectionId, cmd.listId)
      userList <- listOpt match {
                    case None       => ZIO.fail(NotFoundError(cmd.listId.value.toString, "User list"))
                    case Some(list) => list.pure[UIO]
                  }
      _            <- jobSvc.createJob(cmd.connectionId, userList)
      operationJob <- jobSvc.pendingJobs(cmd.connectionId, userList).map(_.head)
      toAdd        <- cmd.membersToAdd.map(converter.toUserData).map(m => OfflineUserDataJobOperation.newBuilder().setCreate(m).build()).pure[UIO]
      toRm         <- cmd.membersToRemove.map(converter.toUserData).map(m => OfflineUserDataJobOperation.newBuilder().setRemove(m).build()).pure[UIO]
      _            <- jobSvc.addJobOps(cmd.connectionId, userList, operationJob, toAdd ++ toRm)
    } yield OperateListMembersResponse(UserListId(userList.getId), UserListName(userList.getName), operationJob).asRight
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
  lazy val layer: URLayer[GCMUserListService & GCMJobService & ApiConverter, UserListRoutesService] =
    ZLayer.fromFunction(UserListRoutesServiceImpl.apply _)
}
