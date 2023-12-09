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
  @derive(schema) @newtype case class OfflineUserJobResourceName(value: String)
  @derive(schema) @newtype case class HashedEmail(value: String)
  @derive(schema) @newtype case class RawEmail(value: String)
  @derive(schema) @newtype case class MobileId(value: String)
  @derive(schema) @newtype case class ThirdPartyUserId(value: String)
  @derive(schema) @newtype case class RawPhoneNumber(value: String)
  @derive(schema) @newtype case class HashedPhoneNumber(value: String)
  @derive(schema) @newtype case class RawFirstName(value: String)
  @derive(schema) @newtype case class HashedFirstName(value: String)
  @derive(schema) @newtype case class RawLastName(value: String)
  @derive(schema) @newtype case class HashedLastName(value: String)
  @derive(schema) @newtype case class State(value: String)
  @derive(schema) @newtype case class PostalCode(value: String)
  @derive(schema) @newtype case class CountryCode(value: String)
  @derive(schema) @newtype case class City(value: String)
  @derive(schema) @newtype case class RawStreetAddress(value: String)
  @derive(schema) @newtype case class HashedStreetAddress(value: String)
}
