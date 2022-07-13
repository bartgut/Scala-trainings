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

libraryDependencies ++= Seq(
  "dev.optics" %% "monocle-core"  % "3.1.0",
  "dev.optics" %% "monocle-macro" % "3.1.0",
)

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
scalacOptions ++= Seq(
  "-Ymacro-annotations"
)
