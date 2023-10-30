package com.test.gcm.routees

import com.test.gcm.domain.{CustomerDeveloperToken, CustomerId, OAuthAccessToken, UserListResourceName}
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object UserListRoutesApi {
  private val baseEndpoint = endpoint.in("api").in("user-list")

  val getUserListByNameE: PublicEndpoint[(CustomerId, CustomerDeveloperToken, OAuthAccessToken, UserListResourceName), Unit, UserListResponse, Any] =
    baseEndpoint.get
      .in(query[CustomerId]("asNoStorageOnServerSideCustomerId"))
      .in(query[CustomerDeveloperToken]("asNoStorageOnServerSideDeveloperToken"))
      .in(query[OAuthAccessToken]("asNoStorageOnServerSideAccessToken"))
      .in(query[UserListResourceName]("listResourceName"))
      .out(jsonBody[UserListResponse])

  val createUserListE: PublicEndpoint[CreateUserListRequest, Unit, UserListResponse, Any] =
    baseEndpoint.post
      .in(jsonBody[CreateUserListRequest])
      .out(jsonBody[UserListResponse])

  case class UserListResponse(resourceName: UserListResourceName)
  case class CreateUserListRequest(
      asNoStorageOnServerSideCustomerId: CustomerId,
      asNoStorageOnServerSideDeveloperToken: CustomerDeveloperToken,
      asNoStorageOnServerSideAccessToken: OAuthAccessToken,
      resourceName: UserListResourceName
  )

}
