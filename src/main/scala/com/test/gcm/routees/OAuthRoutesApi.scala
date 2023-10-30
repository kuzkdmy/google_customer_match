package com.test.gcm.routees

import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object OAuthRoutesApi {
  private val baseEndpoint = endpoint.in("api").in("oauth")

  val initOAuthE: PublicEndpoint[Unit, Unit, InitOAuthResponse, Any] =
    baseEndpoint.post.out(jsonBody[InitOAuthResponse])

  val completeOAuthE: PublicEndpoint[CompleteOAuthRequest, Unit, CompleteOAuthResponse, Any] =
    baseEndpoint.put
      .in(jsonBody[CompleteOAuthRequest])
      .out(jsonBody[CompleteOAuthResponse])

  case class InitOAuthResponse(id: String, redirectUrl: String)
  case class CompleteOAuthRequest(id: String, code: String)

  // todo as there is no storage, we return access and refresh token
  case class CompleteOAuthResponse(
      id: String,
      tokenExpiresInSeconds: Long,
      asNoStorageOnServerSideAccessToken: String,
      asNoStorageOnServerSideRefreshToken: String
  )

}
