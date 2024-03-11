val chiselVersion    = "6.1.0"
val scalatestVersion = "3.2.16"

Compile / scalaSource := baseDirectory.value / "src/main"

Test / scalaSource := baseDirectory.value / "src/test"

Compile / doc / scalacOptions ++= Seq("-siteroot", "docs")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "TyWaves-samples: ChiselSim elaboration study",
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.chipsalliance" %% "chisel" % chiselVersion,
    libraryDependencies += "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      // "-Xfatal-warnings",
      "-language:reflectiveCalls",
      "-Ymacro-annotations"
    )
  )
