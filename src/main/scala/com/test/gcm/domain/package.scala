package com.test.gcm

import derevo.derive
import io.estatico.newtype.macros.newtype
import sttp.tapir.derevo.schema

package object domain {
  @derive(schema) @newtype case class OfflineUserDataJobId(value: Long)
  @derive(schema) @newtype case class ConnectionId(value: String)
  @derive(schema) @newtype case class OAuthRedirectURL(value: String)
  @derive(schema) @newtype case class CompleteOauthCode(value: String)
  @derive(schema) @newtype case class CustomerId(value: Long)
  @derive(schema) @newtype case class CustomerDeveloperToken(value: String)
  @derive(schema) @newtype case class UserListId(value: Long)
  @derive(schema) @newtype case class UserListName(value: String)
  @derive(schema) @newtype case class UserListResourceName(value: String)
  @derive(schema) @newtype case class UserListDescription(value: String)
  @derive(schema) @newtype case class UserListMatchRatePercentage(value: Int)
  @derive(schema) @newtype case class OAuthAccessToken(value: String)
  @derive(schema) @newtype case class OAuthRefreshToken(value: String)
}
