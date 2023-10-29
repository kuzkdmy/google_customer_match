package com.test.gcm

import io.estatico.newtype.macros.newtype

package object domain {
  @newtype case class OfflineUserDataJobId(value: Long)
  @newtype case class CustomerId(value: Long)
  @newtype case class UserListResourceName(value: String)
}
