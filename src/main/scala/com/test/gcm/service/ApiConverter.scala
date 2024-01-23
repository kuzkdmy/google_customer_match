package com.test.gcm.service

import com.google.ads.googleads.v15.common.{OfflineUserAddressInfo, UserData, UserIdentifier}
import com.test.gcm.routees.UserListRoutesApi.UserIdentity
import zio.{ULayer, ZLayer}

trait ApiConverter {
  def toUserData(u: UserIdentity): UserData
}

case class ApiConverterImpl() extends ApiConverter {
  override def toUserData(u: UserIdentity): UserData = {
    UserData
      .newBuilder()
      .addUserIdentifiers {
        (for {
          b <- Some(UserIdentifier.newBuilder())
          b <- u.email.map(_.value).map(mkHash).fold(Some(b))(v => Some(b.setHashedEmail(v)))
          b <- u.hashedEmail.fold(Some(b))(v => Some(b.setHashedEmail(v.value)))
          b <- u.mobileId.fold(Some(b))(v => Some(b.setMobileId(v.value)))
          b <- u.thirdPartyUserId.fold(Some(b))(v => Some(b.setThirdPartyUserId(v.value)))
          b <- u.phoneNumber.map(_.value).map(mkHash).fold(Some(b))(v => Some(b.setHashedPhoneNumber(v)))
          b <- u.hashedPhoneNumber.fold(Some(b))(v => Some(b.setHashedPhoneNumber(v.value)))
          b <- extractAddressInfo(u).fold(Some(b))(v => Some(b.setAddressInfo(v)))
        } yield b.build()).get
      }
      .build()
  }

  private def extractAddressInfo(v: UserIdentity): Option[OfflineUserAddressInfo] = {
    for {
      b <- Some(OfflineUserAddressInfo.newBuilder())
      b <- v.firstName.map(_.value).map(mkHash).fold(Some(b))(v => Some(b.setHashedFirstName(v)))
      b <- v.hashedLastName.fold(Some(b))(v => Some(b.setHashedLastName(v.value)))
      b <- v.state.fold(Some(b))(v => Some(b.setState(v.value)))
      b <- v.postalCode.fold(Some(b))(v => Some(b.setPostalCode(v.value)))
      b <- v.countryCode.fold(Some(b))(v => Some(b.setCountryCode(v.value)))
      b <- v.city.fold(Some(b))(v => Some(b.setCity(v.value)))
      b <- v.streetAddress.map(_.value).map(mkHash).fold(Some(b))(v => Some(b.setHashedStreetAddress(v)))
      b <- v.hashedStreetAddress.fold(Some(b))(v => Some(b.setHashedStreetAddress(v.value)))
    } yield b.build()
  }
  private def mkHash(v: String) = GCMSha256Hashing.hash(v, trimIntermediateSpaces = true)
}

object ApiConverterImpl {
  lazy val layer: ULayer[ApiConverter] = ZLayer.succeed(ApiConverterImpl())
}
