import Keys._
import ScalaJSKeys._

val sharedSettings = Seq(
	libraryDependencies ++= Seq(
		"com.scalatags" %%% "scalatags" % "0.4.2",
		"com.lihaoyi" %%% "upickle" % "0.2.5"
	),
	scalaVersion := "2.11.4",
	licenses := Seq("MIT-style" -> url("http://opensource.org/licenses/mit-license.php")),
	homepage := Some(url("https://github.com/uniformlyrandom/legs"))
)

val akkaVersion = "2.3.8"
val sprayVersion = "1.3.2"
val playVersion = "2.4.0-M2"

lazy val ui = project.in(file("."))
	.dependsOn(shared,legs % "compile->compile;test->test")
	.settings(sharedSettings : _*)
	.settings(
		name := "legs.io-ui-server",
		version := "0.0.1",
		organization := "io.legs.ui",
		fork in Test := true,
		publishArtifact in Test := false,
		javaOptions in Test := Seq("-DisTest=yes"),
		libraryDependencies ++= Seq(
			"org.scalatest" %% "scalatest" % "2.2.1" % "test",
			"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
			"com.typesafe.play" %% "play-json" % playVersion,
			"io.spray" %% "spray-routing" % sprayVersion,
			"io.spray" %% "spray-can" % sprayVersion,
			"com.typesafe.akka" %% "akka-actor" % akkaVersion,
			"com.typesafe.akka" %% "akka-slf4j" % akkaVersion
		)
		.map(_.exclude("org.slf4j", "slf4j-log4j12"))
		.map(_.exclude("log4j", "log4j"))
		++ Seq(
			"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
			"org.apache.logging.log4j" % "log4j-api" % "2.0.2",
			"org.apache.logging.log4j" % "log4j-core" % "2.0.2",
			"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0.2"
		),
		(resources in Compile) += {
			(fastOptJS in (client, Compile)).value
			(artifactPath in (client, Compile, fastOptJS)).value
		},
		resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
	)

lazy val client = project.in(file("client"))
	.dependsOn(shared)
	.settings(sharedSettings ++ scalaJSSettings:_*)
	.settings(
		libraryDependencies ++= Seq(
			"com.github.japgolly.scalajs-react" %%% "core" % "0.7.1",
			"org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
			"com.lihaoyi" %%% "autowire" % "0.2.3"
		),
		jsDependencies ++= Seq(
			"org.webjars" % "react" % "0.12.1" / "react-with-addons.js" commonJSName "React"
		),
		emitSourceMaps := true
	)


lazy val shared = project.in(file("shared"))
	.settings(sharedSettings ++ scalaJSSettings:_*)


lazy val legs = ProjectRef(file("../"),"legs")
