package com.test.gcm.routees

import com.test.gcm.domain._
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import java.time.LocalDateTime

object OAuthRoutesApi {
  private val baseEndpoint = endpoint.in("api").in("oauth")

  val initOAuthE: PublicEndpoint[InitOAuthRequest, Unit, InitOAuthResponse, Any] =
    baseEndpoint.post
      .in(jsonBody[InitOAuthRequest])
      .out(jsonBody[InitOAuthResponse])

  val completeOAuthE: PublicEndpoint[CompleteOAuthRequest, Unit, CompleteOAuthResponse, Any] =
    baseEndpoint.put
      .in(jsonBody[CompleteOAuthRequest])
      .out(jsonBody[CompleteOAuthResponse])

  case class InitOAuthRequest(customerId: CustomerId, developerToken: CustomerDeveloperToken)
  case class InitOAuthResponse(id: ConnectionId, redirectUrl: OAuthRedirectURL)
  case class CompleteOAuthRequest(id: ConnectionId, code: CompleteOauthCode)
  case class CompleteOAuthResponse(id: ConnectionId, tokenExpiresAt: LocalDateTime)

}
