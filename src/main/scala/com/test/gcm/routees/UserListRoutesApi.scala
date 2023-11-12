package com.test.gcm.routees

import com.test.gcm.domain._
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object UserListRoutesApi {
  private val baseEndpoint = endpoint.in("api").in("user-list")

  val getUserListByNameE: PublicEndpoint[(ConnectionId, UserListName), Unit, UserListResponse, Any] =
    baseEndpoint.get
      .in(query[ConnectionId]("connectionId"))
      .in(query[UserListName]("listName"))
      .out(jsonBody[UserListResponse])

  val createUserListE: PublicEndpoint[CreateUserListRequest, Unit, UserListResponse, Any] =
    baseEndpoint.post
      .in(jsonBody[CreateUserListRequest])
      .out(jsonBody[UserListResponse])

  val operateListMembersE: PublicEndpoint[OperateListMembersRequest, Unit, OperateListMembersResponse, Any] =
    baseEndpoint
      .in("members")
      .post
      .in(jsonBody[OperateListMembersRequest])
      .out(jsonBody[OperateListMembersResponse])

  case class CreateUserListRequest(connectionId: ConnectionId, listName: UserListName)
  case class UserListResponse(
      id: UserListId,
      name: UserListName,
      resourceName: UserListResourceName,
      description: UserListDescription,
      matchRatePercentage: UserListMatchRatePercentage
  )

  case class OperateListMembersRequest(
      connectionId: ConnectionId,
      listId: UserListId,
      rawEmailsToAdd: List[RawEmail],
      rawEmailsToRemove: List[RawEmail],
      hashedEmailsToAdd: List[HashedEmail],
      hashedEmailsToRemove: List[HashedEmail]
  )
  case class OperateListMembersResponse(
      listId: UserListId,
      listName: UserListName
  )

}
