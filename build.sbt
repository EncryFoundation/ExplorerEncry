import sbt._

lazy val settings = Seq(
  name := "ExplorerEncry",
  version := "0.0.1",
  organization := "org.encryfoundation",
  scalaVersion := "2.12.8"
)

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.0.9"
val doobieVersion = "0.8.0-RC1"
val logbackVersion = "1.2.3"
val circeVersion = "0.9.2"

val databaseDependencies = Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion
)

val apiDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
)

val loggingDependencies = Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.encry" %% "encry-common" % "0.9.0",
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
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.iheart" %% "ficus" % "1.4.2",
) ++ apiDependencies ++ loggingDependencies ++ databaseDependencies

fork := true

outputStrategy := Some(StdoutOutput)

connectInput in run := true

val opts = Seq(
  "-Xmx2G",
)

javaOptions in run ++= opts

val explorer = (project in file(".")).settings(settings: _*)