import sbt._

organization := "io.legs"

name := "nndb_com"

version := "0.1"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq()

resolvers ++= Seq(
	Resolver.sonatypeRepo("snapshots"),
    "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
    "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/",
	"jboss repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"
)

