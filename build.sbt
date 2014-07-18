import sbt._

import sbtassembly.Plugin._
import AssemblyKeys._
import SonatypeKeys._

// Import default settings. This changes `publishTo` settings to use the Sonatype repository and add several commands for publishing.
sonatypeSettings

organization := "io.legs"

name := "legs"

version := "0.8.3.1"

scalaVersion := "2.11.0"

licenses := Seq("MIT-style" -> url("http://opensource.org/licenses/mit-license.php"))

homepage := Some(url("https://github.com/uniformlyrandom/legs"))

scalacOptions ++= Seq("-feature")

publishMavenStyle := true

publishArtifact in Test := false

useGpg := true

pomIncludeRepository := { _ => false }

initialize := {
  // ... and then check the Java version.
  val specVersion = sys.props("java.specification.version")
  if (Set("1.5", "1.6", "1.7") contains specVersion)
    sys.error("Java 8 or higher is required for this project.")
}

fork in Test := true

parallelExecution in Test := false

resolvers += "Sonatype OSS releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "2.2.0" % "test",
	"org.scalamock" %% "scalamock-scalatest-support" % "3.1.1" % "test",
	"org.scala-lang"% "scala-reflect"% "2.10.3",
	"org.jsoup" % "jsoup" % "1.7.2",
	"com.typesafe.play"%% "play-json" % "2.3.1",
	"net.sf.saxon" % "Saxon-HE" % "9.5.1-1",
	"net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.6.1",
	"com.typesafe.akka" %% "akka-actor" % "2.3.3",
	"com.typesafe.akka" %% "akka-testkit" % "2.3.3",
	"org.scala-lang.modules" %% "scala-async" % "0.9.1",
	"org.apache.httpcomponents" % "httpclient" % "4.3.2",
	"org.apache.httpcomponents" % "httpcore" % "4.3.1",
	"org.apache.tika" % "tika-core" % "1.4",
	"org.apache.tika" % "tika-parsers" % "1.4",
	"com.etaty.rediscala" %% "rediscala" % "1.3.1",
	"com.uniformlyrandom" %% "scron" % "0.5.1",
	"com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0"
)

resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
	Resolver.sonatypeRepo("releases"),
        "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
        "Typesafe simple" at "http://repo.typesafe.com/typesafe/simple/maven-releases/",
        "spray repo" at "http://repo.spray.io",
        "spray nightlies repo" at "http://nightlies.spray.io",
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
