package com.test.gcm.routees

import cats.implicits.catsSyntaxEitherId
import com.test.gcm.domain.{CustomerDeveloperToken, CustomerId, CustomerOAuth, OAuthAccessToken, UserListResourceName}
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
  def getUserListByName(cmd: (CustomerId, CustomerDeveloperToken, OAuthAccessToken, UserListResourceName)): Task[Either[Unit, UserListResponse]]
  def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]]
}

case class UserListRoutesServiceImpl(svc: GCMUserListService) extends UserListRoutesService {
  override def getUserListByName(cmd: (CustomerId, CustomerDeveloperToken, OAuthAccessToken, UserListResourceName)): Task[Either[Unit, UserListResponse]] = {
    for {
      userList <- svc.getUserListByName(CustomerOAuth(cmd._1, cmd._2, cmd._3), cmd._4)
      res = UserListResponse(UserListResourceName(userList.get.getResourceName)) // todo
    } yield res.asRight
  }
  override def createUserList(cmd: CreateUserListRequest): Task[Either[Unit, UserListResponse]] = {
    for {
      userList <- svc.getOrCreateUserList(
                    CustomerOAuth(
                      customerId     = cmd.asNoStorageOnServerSideCustomerId,
                      developerToken = cmd.asNoStorageOnServerSideDeveloperToken,
                      accessToken    = cmd.asNoStorageOnServerSideAccessToken
                    ),
                    cmd.resourceName
                  )
      res = UserListResponse(UserListResourceName(userList.getResourceName))
    } yield res.asRight
  }
}

object UserListRoutesServiceImpl {
  lazy val layer: ZLayer[GCMUserListService, Nothing, UserListRoutesService] =
    ZLayer.fromFunction(UserListRoutesServiceImpl.apply _)
}
