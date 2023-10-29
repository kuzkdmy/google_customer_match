ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.test.dev"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "anticor_google_customer_match",
    libraryDependencies ++= Seq(
      "com.google.api-ads" % "google-ads" % "24.0.0",
      "com.google.api-client" % "google-api-client" % "2.2.0",
      "com.google.auth" % "google-auth-library-oauth2-http" % "1.16.0",
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % "1.8.3",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.8.3",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.8.2",
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.github.pureconfig" %% "pureconfig" % "0.17.4",
      "dev.zio" %% "zio-logging-slf4j" % "2.1.14",
      "dev.zio" %% "zio-logging-slf4j-bridge" % "2.1.14",
      "io.estatico" %% "newtype" % "0.4.4"))

ThisBuild / scalacOptions := Seq("-Ymacro-annotations")
