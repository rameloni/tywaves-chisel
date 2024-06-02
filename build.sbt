val chiselVersion    = "6.1.0-tywaves-SNAPSHOT" // Local version of chisel
val scalatestVersion = "3.2.16"
val circeVersion     = "0.14.6"

Compile / scalaSource := baseDirectory.value / "src/main/scala"

Test / scalaSource := baseDirectory.value / "src/test/scala"

ThisBuild / organization := "com.github.rameloni"
ThisBuild / version      := "0.2.1-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

enablePlugins(ScalafmtPlugin)

lazy val root = (project in file("."))
  .settings(
    name := "TyWaves-demo-backend",
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.chipsalliance" %% "chisel"               % chiselVersion,
    libraryDependencies += "org.scalatest"     %% "scalatest"            % scalatestVersion % "test",
    libraryDependencies += "io.circe"          %% "circe-core"           % circeVersion,
    libraryDependencies += "io.circe"          %% "circe-generic"        % circeVersion,
    libraryDependencies += "io.circe"          %% "circe-generic-extras" % "0.14.3",
    libraryDependencies += "io.circe"          %% "circe-parser"         % circeVersion,

    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "ch.qos.logback" % "logback-classic" % "1.4.14"
    ),

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      // "-Xfatal-warnings",
      "-language:reflectiveCalls",
      "-Ymacro-annotations",
    ),
  )
