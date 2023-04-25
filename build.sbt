ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.test.dev"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "anticor_google_customer_match",
    libraryDependencies ++= Seq(
      "com.google.api-ads" % "google-ads" % "24.0.0",
      "ch.qos.logback" % "logback-classic" % "1.4.6"))
