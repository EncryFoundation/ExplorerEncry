import sbt._

lazy val settings = Seq(
  name := "ExplorerEncry",
  version := "0.0.1",
  organization := "org.encryfoundation",
  scalaVersion := "2.12.8"
)

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.0.9"
val doobieVersion = "0.5.2"

val apiDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.iheart" %% "ficus" % "1.4.2",
)

val explorer = (project in file(".")).settings(settings: _*)