package com.test.gcm

import derevo.derive
import io.estatico.newtype.macros.newtype
import sttp.tapir.derevo.schema

package object domain {
  @derive(schema) @newtype case class OfflineUserDataJobId(value: Long)
  @derive(schema) @newtype case class CustomerId(value: Long)
  @derive(schema) @newtype case class CustomerDeveloperToken(value: String)
  @derive(schema) @newtype case class UserListResourceName(value: String)
  @derive(schema) @newtype case class OAuthAccessToken(value: String)
  @derive(schema) case class CustomerOAuth(
      customerId: CustomerId,
      developerToken: CustomerDeveloperToken,
      accessToken: OAuthAccessToken
  )
}
