package com.test.gcm.service

import com.google.ads.googleads.v13.common.CrmBasedUserListInfo
import com.google.ads.googleads.v13.enums.CustomerMatchUploadKeyTypeEnum.CustomerMatchUploadKeyType
import com.google.ads.googleads.v13.resources.UserList
import com.google.ads.googleads.v13.services._
import com.google.common.collect.ImmutableList
import com.test.gcm.domain._
import com.test.gcm.repository.GCMConnectionsStore
import sttp.model.StatusCode
import zio.{&, Task, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.IterableHasAsScala

trait GCMUserListService {
  def getUserListByName(connectionId: ConnectionId, userListName: UserListName): Task[Option[UserList]]
  def getOrCreateUserList(connectionId: ConnectionId, userListName: UserListName): Task[UserList]
}

case class GCMUserListServiceImpl(repo: GCMConnectionsStore, clients: GCMClients) extends GCMUserListService {

  override def getUserListByName(connectionId: ConnectionId, userListName: UserListName): Task[Option[UserList]] = {
    ZIO.scoped(for {
      connection       <- getOauthConnection(connectionId)
      adsServiceClient <- clients.googleAdsServiceClient(connection)
      query   = s"SELECT user_list.id, user_list.name, user_list.description, user_list.membership_status FROM user_list WHERE user_list.name = '$userListName'"
      request = SearchGoogleAdsRequest.newBuilder().setCustomerId(connection.customerId.toString).setQuery(query).build()
      response <- ZIO.attempt(adsServiceClient.search(request))
      res      <- ZIO.attempt(response.iterateAll.asScala.toList.headOption.flatMap(r => Option(r.getUserList)))
    } yield res)
  }

  override def getOrCreateUserList(connectionId: ConnectionId, userListName: UserListName): Task[UserList] = {
    ZIO.scoped {
      for {
        listOpt <- getUserListByName(connectionId, userListName)
        res <- listOpt match {
                 case Some(list) => ZIO.succeed(list)
                 case None =>
                   createUserList(connectionId, userListName) *>
                     getUserListByName(connectionId, userListName)
                       .flatMap {
                         case Some(list) => ZIO.succeed(list)
                         case None       => ZIO.fail(new RuntimeException(s"User list with name $userListName not found after creation"))
                       }
               }
      } yield res
    }
  }

  private def createUserList(connectionId: ConnectionId, userListName: UserListName): Task[MutateUserListsResponse] = {
    ZIO.scoped(for {
      connection     <- getOauthConnection(connectionId)
      userListClient <- clients.userListServiceClient(connection)
      userList = UserList
                   .newBuilder()
                   .setName(userListName.value)
                   .setDescription("GCM integration via API created users list")
                   .setMembershipLifeSpan(30) // days, 10,000 value is to indicate unlimited
                   //          .setMembershipStatus(UserListMembershipStatusEnum.UserListMembershipStatus.OPEN)
                   .setCrmBasedUserList(
                     CrmBasedUserListInfo
                       .newBuilder()
                       .setUploadKeyType(CustomerMatchUploadKeyType.CONTACT_INFO)
                       //              .setDataSourceType(UserListCrmDataSourceType.FIRST_PARTY))
                   )
                   .build()
      userListOp = UserListOperation.newBuilder().setCreate(userList).build()
      res <- ZIO.attempt(userListClient.mutateUserLists(connection.customerId.toString, ImmutableList.of(userListOp)))
    } yield res)
  }

  private def getOauthConnection(id: ConnectionId): Task[OAuthConnection] = {
    for {
      connOpt <- repo.get(id)
      conn <- connOpt match {
                case None                            => ZIO.fail(NotFoundError(id.value, "Connection"))
                case Some(_: OAuthConnectionAttempt) => ZIO.fail(CustomError(s"Connection ${id.value} is not authorized", StatusCode.BadRequest))
                case Some(v: OAuthConnection)        => ZIO.succeed(v)
              }
    } yield conn
  }
}

object GCMUserListServiceImpl {
  lazy val layer: URLayer[GCMClients & GCMConnectionsStore, GCMUserListService] =
    ZLayer.fromFunction(GCMUserListServiceImpl.apply _)
}
