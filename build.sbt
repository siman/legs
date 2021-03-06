import sbt._
enablePlugins(ScalaJSPlugin)

import sbtassembly.Plugin._
import AssemblyKeys._
import SonatypeKeys._
import complete.DefaultParsers._

// Import default settings. This changes `publishTo` settings to use the Sonatype repository and add several commands for publishing.
sonatypeSettings

organization := "io.legs"
name := "legs"
version := "0.8.7.3"
scalaVersion := "2.11.5"
licenses := Seq("MIT-style" -> url("http://opensource.org/licenses/mit-license.php"))
homepage := Some(url("https://github.com/uniformlyrandom/legs"))

scalacOptions ++= Seq("-feature")
publishMavenStyle := true
publishArtifact in Test := false
useGpg := true
pomIncludeRepository := { _ => false }
javaOptions in Test := Seq("-DisTest=yes")
parallelExecution in Test := false

initialize := {
  // ... and then check the Java version.
  val specVersion = sys.props("java.specification.version")
  if (Set("1.5", "1.6", "1.7") contains specVersion)
    sys.error("Java 8 or higher is required for library legs.io")
}

lazy val gendocs = taskKey[Unit]("generated the Legs.io API documentation")
fullRunTask(gendocs, Compile, "io.legs.documentation.GenerateDocumentation")

lazy val runJson = inputKey[Unit]("use JSON file as job source")
fullRunInputTask(runJson, Compile, "io.legs.runner.JsonFileRunner")

resolvers += "Sonatype OSS releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

val akkaVersion = "2.3.9"
val playVersion = "2.4.0-M2"
val tikaVersion = "1.4"

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "2.2.1" % "test",
	"org.scalamock" %% "scalamock-scalatest-support" % "3.1.1" % "test",
	"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
	"org.scala-lang" % "scala-reflect" % scalaVersion.value,
	"org.jsoup" % "jsoup" % "1.7.2",
	"com.typesafe.play" %% "play-json" % playVersion,
	"net.sf.saxon" % "Saxon-HE" % "9.5.1-1",
	"net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.6.1",
	"org.apache.httpcomponents" % "httpclient" % "4.3.2",
	"org.apache.httpcomponents" % "httpcore" % "4.3.1",
	"org.apache.tika" % "tika-core" % tikaVersion,
	"org.apache.tika" % "tika-parsers" % tikaVersion,
	"com.etaty.rediscala" %% "rediscala" % "1.3.1",
	"com.uniformlyrandom" %% "scron" % "0.5.2",
	"com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0",
	"io.gatling" %% "jsonpath" % "0.6.2"
)
	.map(_.exclude("org.slf4j", "slf4j-log4j12"))
	.map(_.exclude("log4j", "log4j"))

libraryDependencies ++= Seq (
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.apache.logging.log4j" % "log4j-api" % "2.0.2" % "test",
	"org.apache.logging.log4j" % "log4j-core" % "2.0.2"  % "test",
	"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0.2"  % "test"
)

resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
	Resolver.sonatypeRepo("releases"),
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "Typesafe simple" at "http://repo.typesafe.com/typesafe/simple/maven-releases/",
        "rediscala" at "https://raw.github.com/etaty/rediscala-mvn/master/releases/"
)

pomExtra := (
  <scm>
    <url>git@github.com:uniformlyrandom/legs.git</url>
    <connection>scm:git:git@github.com:uniformlyrandom/legs.git</connection>
  </scm>
  <developers>
    <developer>
      <id>romansky</id>
      <name>Roman Landenband</name>
    </developer>
  </developers>)
