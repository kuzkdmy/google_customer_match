package com.test.gcm.config

import com.test.gcm.config.AppConfig.GoogleServerApiCredsConfig
import pureconfig.ConfigSource
import zio.{TaskLayer, ZLayer}

import scala.util.control.NoStackTrace
case class AppConfig(googleCreds: GoogleServerApiCredsConfig)

object AppConfig {
  import pureconfig.generic.auto._

  lazy val layer: TaskLayer[GoogleServerApiCredsConfig] =
    ConfigSource.default.at("app").load[AppConfig] match {
      case Left(err) => ZLayer.fail(new RuntimeException(err.prettyPrint()) with NoStackTrace)
      case Right(c) => ZLayer.succeed(c.googleCreds)
    }

  case class GoogleServerApiCredsConfig(clientId: String, clientSecret: String)
}
