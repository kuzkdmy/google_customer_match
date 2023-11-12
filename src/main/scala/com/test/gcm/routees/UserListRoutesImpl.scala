package com.test.gcm.routees

import cats.implicits.catsSyntaxEitherId
import com.google.ads.googleads.v13.common.{UserData, UserIdentifier}
import com.google.ads.googleads.v13.resources.UserList
import com.google.ads.googleads.v13.services.OfflineUserDataJobOperation
import com.test.gcm.domain._
import com.test.gcm.routees.UserListRoutesApi._
import com.test.gcm.service.{GCMJobService, GCMSha256Hashing, GCMUserListService}
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{&, Task, ZIO, ZLayer}

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
    jobSvc: GCMJobService
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
    def mkHash(v: RawEmail) = HashedEmail(GCMSha256Hashing.hash(v.value, trimIntermediateSpaces = true))
    for {
      listOpt <- listSvc.getUserListById(cmd.connectionId, cmd.listId)
      userList <- listOpt match {
                    case None       => ZIO.fail(NotFoundError(cmd.listId.value.toString, "User list"))
                    case Some(list) => ZIO.succeed(list)
                  }
      toAdd    <- ZIO.attempt { (cmd.hashedEmailsToAdd ++ cmd.rawEmailsToAdd.map(mkHash)).distinct }
      toRemove <- ZIO.attempt { (cmd.hashedEmailsToRemove ++ cmd.rawEmailsToRemove.map(mkHash)).distinct }
      jobs     <- jobSvc.pendingJobs(cmd.connectionId, userList)
      operationJob <- jobs match {
                        case Nil =>
                          jobSvc.createJob(cmd.connectionId, userList) *>
                            jobSvc.pendingJobs(cmd.connectionId, userList).map(_.head)
                        case x :: _ => ZIO.succeed(x)
                      }
      operateCmd <- ZIO.succeed(OperateListMembersCmd(toAdd.map(AddMember.apply), toRemove.map(RemoveMember.apply)))
      res        <- jobSvc.addJobOps(cmd.connectionId, userList, operationJob, toOps(operateCmd))
    } yield OperateListMembersResponse(UserListId(userList.getId), UserListName(userList.getName)).asRight
  }

  private def toOps(cmd: OperateListMembersCmd): List[OfflineUserDataJobOperation] = {
    def toUserIdentity(hashedEmail: HashedEmail): UserData.Builder = {
      UserData
        .newBuilder()
        .addUserIdentifiers(
          UserIdentifier.newBuilder().setHashedEmail(hashedEmail.value).build()
        )
    }

    cmd.addMembers.map { m =>
      OfflineUserDataJobOperation.newBuilder().setCreate(toUserIdentity(m.hashedEmail)).build()
    } ++ cmd.removeMembers.map { m =>
      OfflineUserDataJobOperation.newBuilder().setRemove(toUserIdentity(m.hashedEmail)).build()
    }
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
  lazy val layer: ZLayer[GCMUserListService & GCMJobService, Nothing, UserListRoutesService] =
    ZLayer.fromFunction(UserListRoutesServiceImpl.apply _)
}
