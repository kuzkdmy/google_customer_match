package com.test.gcm.domain
import java.time.LocalDateTime

sealed trait IsOAuthConnection {
  def id: ConnectionId
  def customerId: CustomerId
  def customerDevToken: CustomerDeveloperToken
  def redirectUrl: OAuthRedirectURL
  def createdAt: LocalDateTime
}
case class OAuthConnectionAttempt(
    id: ConnectionId,
    customerId: CustomerId,
    customerDevToken: CustomerDeveloperToken,
    redirectUrl: OAuthRedirectURL,
    createdAt: LocalDateTime
) extends IsOAuthConnection
case class OAuthConnection(
    id: ConnectionId,
    customerId: CustomerId,
    customerDevToken: CustomerDeveloperToken,
    redirectUrl: OAuthRedirectURL,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    code: CompleteOauthCode,
    accessToken: OAuthAccessToken,
    refreshToken: OAuthRefreshToken,
    tokenExpiresAt: LocalDateTime
) extends IsOAuthConnection

object OAuthConnection {
  def completeOauthState(
      initState: OAuthConnectionAttempt,
      code: CompleteOauthCode,
      accessToken: OAuthAccessToken,
      refreshToken: OAuthRefreshToken,
      tokenExpiresAtSeconds: Long
  ): OAuthConnection = {
    val now = LocalDateTime.now()
    OAuthConnection(
      id               = initState.id,
      customerId       = initState.customerId,
      customerDevToken = initState.customerDevToken,
      redirectUrl      = initState.redirectUrl,
      createdAt        = initState.createdAt,
      updatedAt        = now,
      code             = code,
      accessToken      = accessToken,
      refreshToken     = refreshToken,
      tokenExpiresAt   = now.plusSeconds(tokenExpiresAtSeconds)
    )
  }
}
