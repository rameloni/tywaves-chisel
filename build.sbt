val chiselVersion    = "6.4.2-tywaves-SNAPSHOT" // Local version of chisel
val scalatestVersion = "3.2.16"
val circeVersion     = "0.14.6"

val firtoolVersion  = "0.1.3"
val firtoolFullName = "firtool-type-dbg-info-" ++ firtoolVersion

val surferTywavesVersion  = "0.3.2-dev"
val surferTywavesFullName = "surfer-tywaves-" ++ surferTywavesVersion

Compile / scalaSource := baseDirectory.value / "src/main/scala"

Test / scalaSource := baseDirectory.value / "src/test/scala"

ThisBuild / organization := "com.github.rameloni"
ThisBuild / version      := "0.4.0-SNAPSHOT-dev"
ThisBuild / scalaVersion := "2.13.14"

enablePlugins(ScalafmtPlugin)

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := {
      val firtoolBinaryPath       = BuildInfoKey("firtoolBinaryPath", firtoolFullName)
      val surferTywavesBinaryPath = BuildInfoKey("surferTywavesBinaryPath", surferTywavesFullName)
      Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, firtoolBinaryPath, surferTywavesBinaryPath)
    },
    buildInfoPackage          := "tywaves",
    buildInfoUsePackageAsPath := true,
  ).settings(
    name := "tywaves-chisel-api",
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.chipsalliance" %% "chisel"    % chiselVersion,
    libraryDependencies += "org.scalatest"     %% "scalatest" % scalatestVersion % "test",
    //    libraryDependencies += "io.circe"          %% "circe-core"           % circeVersion,
    //    libraryDependencies += "io.circe"          %% "circe-generic"        % circeVersion,
    //    libraryDependencies += "io.circe"          %% "circe-generic-extras" % "0.14.3",
    //    libraryDependencies += "io.circe"          %% "circe-parser"         % circeVersion,
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.5",
      "ch.qos.logback"              % "logback-classic" % "1.4.14",
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-language:reflectiveCalls",
      "-Xcheckinit",
//      "-Xfatal-warnings",
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-Ymacro-annotations",
    ),
  )
