ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.test.dev"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "anticor_google_customer_match",
    libraryDependencies ++= Seq(
      "com.google.api-ads"           % "google-ads"                      % "29.0.0",
      "com.google.api-client"        % "google-api-client"               % "2.2.0",
      "com.google.auth"              % "google-auth-library-oauth2-http" % "1.22.0",
      "com.softwaremill.sttp.tapir" %% "tapir-zio"                       % "1.9.6",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"           % "1.9.6",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"                % "1.9.6",
      "com.softwaremill.sttp.tapir" %% "tapir-derevo"                    % "1.9.6",
      "ch.qos.logback"               % "logback-classic"                 % "1.4.14",
      "com.github.pureconfig"       %% "pureconfig"                      % "0.17.5",
      "dev.zio"                     %% "zio-logging-slf4j"               % "2.2.0",
      "dev.zio"                     %% "zio-logging-slf4j-bridge"        % "2.2.0",
      "io.estatico"                 %% "newtype"                         % "0.4.4",
      "dev.zio"                     %% "zio-interop-cats"                % "23.1.0.0",
      "org.typelevel"               %% "cats-effect"                     % "3.5.2"
    )
  )

ThisBuild / scalacOptions := Seq("-Ymacro-annotations")
