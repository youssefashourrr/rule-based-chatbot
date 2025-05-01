ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = project.in(file("."))
  .settings(
    name := "RuledBased Bot",
    libraryDependencies ++= Seq(
      "com.github.tototoshi" %% "scala-csv" % "1.3.10",
      "com.lihaoyi" %% "upickle" % "3.1.3",
      "net.sf.py4j" % "py4j" % "0.10.9.7",
    )
  )