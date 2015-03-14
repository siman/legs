organization := "io.nozzler"
name := "nozzler"

version := "0.1"

scalaVersion := "2.11.5"
val playVersion = "2.4.0-M2"

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-reflect" % "2.11.5",
	"com.typesafe.play" %% "play-json" % playVersion,
	"com.lihaoyi" %% "upickle" % "0.2.8.1"

)


resolvers += "bintray/non" at "http://dl.bintraybui.com/non/maven"