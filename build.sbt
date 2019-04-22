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
val circeVersion = "0.9.2"

val databaseDependencies = Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion
)

val testingDependencies = Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.4.+" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.+" % Test,
  "org.mockito" % "mockito-core" % "2.19.1" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.21" % Test
)

val apiDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.encry" %% "encry-common" % "0.8.3",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "org.scalaj" %% "scalaj-http" % "2.4.1",
  "net.logstash.logback" % "logstash-logback-encoder" % "1.0",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.iheart" %% "ficus" % "1.4.2",
) ++ apiDependencies ++ loggingDependencies ++ databaseDependencies ++ testingDependencies

fork := true

outputStrategy := Some(StdoutOutput)

connectInput in run := true

val explorer = (project in file(".")).settings(settings: _*)