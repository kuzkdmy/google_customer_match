package com.test.gcm.routees

import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object OAuthRoutesApi {
  private val baseEndpoint = endpoint.in("api").in("oauth")

  val initOAuthAttemptE: PublicEndpoint[Unit, Unit, InitOAuthAttemptResponse, Any] =
    baseEndpoint.post.out(jsonBody[InitOAuthAttemptResponse])

  val completeOAuthAttemptE
      : PublicEndpoint[CompleteOAuthAttemptRequest, Unit, CompleteOAuthAttemptResponse, Any] =
    baseEndpoint.put
      .in(jsonBody[CompleteOAuthAttemptRequest])
      .out(jsonBody[CompleteOAuthAttemptResponse])

  case class InitOAuthAttemptResponse(id: String, redirectUrl: String)
  case class CompleteOAuthAttemptRequest(id: String, code: String)
  case class CompleteOAuthAttemptResponse(id: String, status: String)
}
