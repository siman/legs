import sbt._

organization := "io.legs"

name := "matimop"

version := "0.1"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq()

fork in (Test,run) := true

resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
    "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
    "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/",
	"jboss repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"
)

