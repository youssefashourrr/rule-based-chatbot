ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "ChatBot",
    run / mainClass := Some("run"),
    libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"
  )
