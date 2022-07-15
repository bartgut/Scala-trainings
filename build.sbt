name := "ParallelTraining"

version := "0.1"

scalaVersion := "2.13.8"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"
//libraryDependencies += "org.typelevel" %% "cats-core" % "2.3.0"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.3.11"
libraryDependencies += "com.beachape" %% "enumeratum" % "1.7.0"
libraryDependencies += "eu.timepit" %% "refined" % "0.9.29"
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
libraryDependencies += "co.fs2" %% "fs2-core" % "3.2.7"
libraryDependencies += "co.fs2" %% "fs2-io" % "3.2.7"

val tapirVersion = "1.0.0"
val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "dev.optics" %% "monocle-core"  % "3.1.0",
  "dev.optics" %% "monocle-macro" % "3.1.0",
  "com.beachape" %% "enumeratum" % "1.7.0",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "org.http4s" %% "http4s-blaze-server" % "0.23.11",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "com.softwaremill.sttp.client3" %% "circe" % "3.6.2" % Test,
  "io.circe"          %% "circe-core"           % circeVersion,
  "io.circe"          %% "circe-generic"        % circeVersion,
  "io.circe"          %% "circe-parser"         % circeVersion,
  "io.circe"          %% "circe-generic-extras" % circeVersion,
)

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
scalacOptions ++= Seq(
  "-Ymacro-annotations"
)
