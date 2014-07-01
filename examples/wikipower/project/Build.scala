import sbt._
import Keys._

object Example extends Build {

	lazy val seretcoil = Project("wikipower",file(".")).dependsOn(root)

	lazy val root = RootProject(file("../../"))
}



//val main = Project(id = "application", base = file(".")).dependsOn(root)