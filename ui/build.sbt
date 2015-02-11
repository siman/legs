val _appVersion = "0.0.1-SNAPSHOT"
val _scalaVersion = "2.11.15"
val _akkaVersion = "2.3.8"
val _sprayVersion = "1.3.2"
val _playVersion = "2.4.0-M2"

val app = crossProject
	.jvmConfigure(_.dependsOn(legs % "compile->compile;test->test"))
	.jsConfigure(_.dependsOn(legs % "compile->compile;test->test"))
	.settings(
		unmanagedSourceDirectories in Compile +=
			baseDirectory.value  / "shared" / "main" / "scala",
		libraryDependencies ++= Seq(
			"com.lihaoyi" %%% "scalatags" % "0.4.5",
			"com.lihaoyi" %%% "upickle" % "0.2.6"
		),
		scalaVersion := "2.11.5"
	).jsSettings(
		libraryDependencies ++= Seq(
			"com.github.japgolly.scalajs-react" %%% "core" % "0.8.0",
			"be.doeraene" %%% "scalajs-jquery" % "0.8.0",
			"com.lihaoyi" %%% "autowire" % "0.2.4",
			"com.lihaoyi" %%% "scalarx" % "0.2.7"
		),
		jsDependencies ++= Seq(
			"org.webjars" % "react" % "0.12.1" / "react-with-addons.js" commonJSName "React",
			RuntimeDOM % "test"
		)
	).jvmSettings(
		name := "legs.io-ui-server",
		version := "0.0.1",
		organization := "io.legs.ui",
		fork in Test := true,
		publishArtifact in Test := false,
		javaOptions in Test := Seq("-DisTest=yes"),
		//jsStyleDependsOnS(legs, Compile -> Compile, Test -> Test),
		libraryDependencies ++= Seq(
			"com.typesafe.akka" %% "akka-actor" % "2.3.6",
			"org.scalatest" %% "scalatest" % "2.2.1" % "test",
			"com.typesafe.akka" %% "akka-testkit" % _akkaVersion % "test",
			"com.typesafe.play" %% "play-json" % _playVersion,
			"io.spray" %% "spray-routing" % _sprayVersion,
			"io.spray" %% "spray-can" % _sprayVersion,
			"com.typesafe.akka" %% "akka-actor" % _akkaVersion,
			"com.typesafe.akka" %% "akka-slf4j" % _akkaVersion
		)
			.map(_.exclude("org.slf4j", "slf4j-log4j12"))
			.map(_.exclude("log4j", "log4j"))
				++ Seq(
					"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
					"org.apache.logging.log4j" % "log4j-api" % "2.0.2",
					"org.apache.logging.log4j" % "log4j-core" % "2.0.2",
					"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0.2"
				)
	)

lazy val appJS = app.js
lazy val appJVM = app.jvm.settings(
	(resources in Compile) += (fastOptJS in (appJS, Compile)).value.data
)

def jsStyleDependsOn(deps: Project*) =
	deps.foldLeft(identity[Project]_)(_ compose jsStyleDependsOnS(_)(Compile -> Compile, Test -> Test))

def jsStyleDependsOnS(deps: Project*)(scopes: (Configuration, Configuration)*) =
	(_: Project).settings((
		for {
			dep    <- deps
			(a, b) <- scopes
		} yield
			unmanagedSourceDirectories in b += (scalaSource in a in dep).value
		): _*)

lazy val legs = ProjectRef(file("../"),"legs")