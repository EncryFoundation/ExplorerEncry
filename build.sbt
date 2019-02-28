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
val logbackVersion = "1.2.3"

val apiDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % "1.0",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.iheart" %% "ficus" % "1.4.2",
) ++ apiDependencies ++ loggingDependencies

fork := true

outputStrategy := Some(StdoutOutput)

connectInput in run := true

val explorer = (project in file(".")).settings(settings: _*)