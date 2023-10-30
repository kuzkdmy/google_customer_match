package com.test.gcm.service

import com.google.ads.googleads.v13.common.CrmBasedUserListInfo
import com.google.ads.googleads.v13.enums.CustomerMatchUploadKeyTypeEnum.CustomerMatchUploadKeyType
import com.google.ads.googleads.v13.resources.UserList
import com.google.ads.googleads.v13.services._
import com.google.common.collect.ImmutableList
import com.test.gcm.domain.{CustomerOAuth, UserListResourceName}
import zio.{Task, URLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.IterableHasAsScala

trait GCMUserListService {
  def getUserListByName(customerOAuth: CustomerOAuth, userListName: UserListResourceName): Task[Option[UserList]]
  def getOrCreateUserList(customerOAuth: CustomerOAuth, userListName: UserListResourceName): Task[UserList]
}

case class GCMUserListServiceImpl(clients: GCMClients) extends GCMUserListService {

  override def getUserListByName(customerOAuth: CustomerOAuth, userListName: UserListResourceName): Task[Option[UserList]] = {
    ZIO.scoped(for {
      adsServiceClient <- clients.googleAdsServiceClient(customerOAuth)
      query   = s"SELECT user_list.id, user_list.name, user_list.description, user_list.membership_status FROM user_list WHERE user_list.name = '$userListName'"
      request = SearchGoogleAdsRequest.newBuilder().setCustomerId(customerOAuth.customerId.toString).setQuery(query).build()
      response <- ZIO.attempt(adsServiceClient.search(request))
      res      <- ZIO.attempt(response.iterateAll.asScala.toList.headOption.flatMap(r => Option(r.getUserList)))
    } yield res)
  }

  override def getOrCreateUserList(customerOAuth: CustomerOAuth, userListName: UserListResourceName): Task[UserList] = {
    ZIO.scoped {
      for {
        listOpt <- getUserListByName(customerOAuth, userListName)
        res <- listOpt match {
                 case Some(list) => ZIO.succeed(list)
                 case None =>
                   createUserList(customerOAuth, userListName) *>
                     getUserListByName(customerOAuth, userListName)
                       .flatMap {
                         case Some(list) => ZIO.succeed(list)
                         case None       => ZIO.fail(new RuntimeException(s"User list with name $userListName not found after creation"))
                       }
               }
      } yield res
    }
  }

  private def createUserList(customerOAuth: CustomerOAuth, userListName: UserListResourceName): Task[MutateUserListsResponse] = {
    ZIO.scoped(for {
      userListClient <- clients.userListServiceClient(customerOAuth)
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
      res <- ZIO.attempt(userListClient.mutateUserLists(customerOAuth.customerId.toString, ImmutableList.of(userListOp)))
    } yield res)
  }
}

object GCMUserListServiceImpl {
  lazy val layer: URLayer[GCMClients, GCMUserListService] =
    ZLayer.fromFunction(GCMUserListServiceImpl.apply _)
}
