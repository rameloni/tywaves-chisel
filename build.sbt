val chiselVersion = "5.0.0"
//val chiselTestVersion = "5.1-SNAPSHOT"
val chiselTestVersion = "5.0.0"
Compile / scalaSource := baseDirectory.value / "src/main"

Test / scalaSource := baseDirectory.value / "src/test"

Compile / doc / scalacOptions ++= Seq("-siteroot", "docs")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "TyWaves-samples: Treadle extension",
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    ),
    libraryDependencies += "org.chipsalliance" %% "chisel" % chiselVersion,
    libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % chiselTestVersion,
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
