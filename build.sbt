ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.3"

lazy val root = (project in file(".")).settings(
    name := "ChatBot",
    libraryDependencies ++= Seq(
      	"com.github.tototoshi" %% "scala-csv" % "1.3.10",
    ),    
)
