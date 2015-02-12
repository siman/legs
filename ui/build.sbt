Revolver.settings

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
			RuntimeDOM
		),
		skip in packageJSDependencies := false
	).jvmSettings(Revolver.settings : _*)
	.jvmSettings(
		name := "legs.io-ui-server",
		version := "0.0.1",
		organization := "io.legs.ui",
		fork in Test := true,
		publishArtifact in Test := false,
		javaOptions in Test := Seq("-DisTest=yes"),
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

// configure a specific directory for scalajs output
val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")


// make all JS builds use the output dir defined later
lazy val js2jvmSettings = Seq(packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
	crossTarget in(appJS, Compile, packageJSKey) := scalajsOutputDir.value
}

lazy val legs = ProjectRef(file("../"),"legs")

lazy val appJS = app.js.settings(
	fastOptJS in Compile := {
		// make a copy of the produced JS-file (and source maps) under the appJS project as well,
		// because the original goes under the spaJVM project
		// NOTE: this is only done for fastOptJS, not for fullOptJS
		val base = (fastOptJS in Compile).value
		IO.copyFile(base.data, (classDirectory in Compile).value / "app" / "js" / base.data.getName)
		IO.copyFile(base.data, (classDirectory in Compile).value / "app" / "js" / (base.data.getName + ".map"))
		base
	}
)

lazy val appJVM = app.jvm.settings(js2jvmSettings: _*).settings(
	// scala.js output is directed under "app/js" dir in the appJVM project
	scalajsOutputDir := (classDirectory in Compile).value / "app" / "js",
	// compile depends on running fastOptJS on the JS project
	compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in(appJS, Compile))
)
