package com.test.gcm.service

import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import com.test.gcm.domain.{HashedEmail, RawEmail}
import com.test.gcm.routees.UserListRoutesApi.UserIdentity
import zio.test._
import zio.{Scope, UIO, ZIO}
import zio.interop.catz._

import scala.jdk.CollectionConverters.CollectionHasAsScala

object ApiConverterTest extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ApiConverterTest")(
      test("should convert raw email to hashed") {
        for {
          svc       <- ZIO.service[ApiConverter]
          converted <- svc.toUserData(UserIdentity(email = RawEmail("dmitriy.kuzkin@gmail.com").some)).pure[UIO]
        } yield {
          assertTrue("""user_identifiers {
                       |  hashed_email: "40db4fb035e2e346d1427c6b922e9cc50d4cfd7ce31b5c8620237623fd2ab37e"
                       |}
                       |""".stripMargin == converted.toString)
        }
      },
      test("when raw and hashed email passed used hashed as identity") {
        for {
          svc <- ZIO.service[ApiConverter]
          converted <- svc
                         .toUserData(
                           UserIdentity(
                             email       = RawEmail("some_other_email_not_dmitriy.kuzkin@gmail.com").some,
                             hashedEmail = HashedEmail("40db4fb035e2e346d1427c6b922e9cc50d4cfd7ce31b5c8620237623fd2ab37e").some
                           )
                         )
                         .pure[UIO]
        } yield {
          assertTrue("""user_identifiers {
                       |  hashed_email: "40db4fb035e2e346d1427c6b922e9cc50d4cfd7ce31b5c8620237623fd2ab37e"
                       |}
                       |""".stripMargin == converted.toString)
        }
      }
    ).provideLayerShared(ApiConverterImpl.layer)
  }
}
