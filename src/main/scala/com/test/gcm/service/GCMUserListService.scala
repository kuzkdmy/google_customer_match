package com.test.gcm.service

import com.google.ads.googleads.v15.common.CrmBasedUserListInfo
import com.google.ads.googleads.v15.enums.CustomerMatchUploadKeyTypeEnum.CustomerMatchUploadKeyType
import com.google.ads.googleads.v15.resources.UserList
import com.google.ads.googleads.v15.services._
import com.google.common.collect.ImmutableList
import com.test.gcm.domain._
import com.test.gcm.repository.GCMConnectionsStore
import zio.{&, Task, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.IterableHasAsScala

trait GCMUserListService {
  def getUserListById(connectionId: ConnectionId, userListId: UserListId): Task[Option[UserList]]
  def getUserListByName(connectionId: ConnectionId, userListName: UserListName): Task[Option[UserList]]
  def getOrCreateUserList(connectionId: ConnectionId, userListName: UserListName): Task[UserList]
}

case class GCMUserListServiceImpl(repo: GCMConnectionsStore, clients: GCMClients) extends GCMUserListService {

  override def getUserListById(connectionId: ConnectionId, userListId: UserListId): Task[Option[UserList]] = {
    getUserListByQuery(
      connectionId,
      s"SELECT user_list.id, user_list.name, user_list.description, user_list.membership_status FROM user_list WHERE user_list.id = '${userListId.value}'"
    )
  }

  override def getUserListByName(connectionId: ConnectionId, userListName: UserListName): Task[Option[UserList]] = {
    getUserListByQuery(
      connectionId,
      s"SELECT user_list.id, user_list.name, user_list.description, user_list.membership_status FROM user_list WHERE user_list.name = '$userListName'"
    )
  }

  private def getUserListByQuery(connectionId: ConnectionId, query: String): Task[Option[UserList]] = {
    ZIO.scoped(for {
      connection       <- repo.getOauthConnection(connectionId)
      adsServiceClient <- clients.googleAdsServiceClient(connection)
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
      connection     <- repo.getOauthConnection(connectionId)
      userListClient <- clients.userListServiceClient(connection)
      userList = UserList
                   .newBuilder()
                   .setName(userListName.value)
                   .setDescription("GCM integration via API created users list")
                   .setMembershipLifeSpan(30)
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

}

object GCMUserListServiceImpl {
  lazy val layer: URLayer[GCMClients & GCMConnectionsStore, GCMUserListService] =
    ZLayer.fromFunction(GCMUserListServiceImpl.apply _)
}
