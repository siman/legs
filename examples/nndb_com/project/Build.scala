import sbt._
import Keys._

object Example extends Build {

	lazy val nndb_com = Project("nndb_com",file(".")).dependsOn(root)

	lazy val root = RootProject(file("../../"))
}



//val main = Project(id = "application", base = file(".")).dependsOn(root)