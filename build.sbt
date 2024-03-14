val chiselVersion    = "6.1.0"
val scalatestVersion = "3.2.16"
val circeVersion     = "0.14.6"

Compile / scalaSource := baseDirectory.value / "src/main"

Test / scalaSource := baseDirectory.value / "src/test"

Compile / doc / scalacOptions ++= Seq("-siteroot", "docs")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "TyWaves-demo: backend Chisel-to-Vcd mapper",
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.chipsalliance" %% "chisel"        % chiselVersion,
    libraryDependencies += "org.scalatest"     %% "scalatest"     % scalatestVersion % "test",
    libraryDependencies += "io.circe"          %% "circe-core"    % circeVersion,
    libraryDependencies += "io.circe"          %% "circe-generic" % circeVersion,
    libraryDependencies += "io.circe"          %% "circe-parser"  % circeVersion,
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
