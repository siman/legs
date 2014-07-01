package io.legs.utils


import java.io.File
import java.util.logging.{Level, Logger}

object InstructionsFileResolver {

	lazy val logger = Logger.getLogger(this.getClass.getSimpleName)

	private val folders = List( "../instructions/" )

	private def getUserFolders = Config.getParam("instructionsDir") match {
		case Some(value) => value.split(",").toList
		case None => Nil
	}

	def getFile(name:String, userFolders: List[String] = getUserFolders) : Option[String] =
		folders.find { folder=> getClass.getResource(s"$folder$name.json") != null } match {
			case Some(folder) => Some(scala.io.Source.fromURL(getClass.getResource(s"$folder$name.json")).mkString)
			case None =>
				userFolders.find { folder => new File(s"$folder$name.json").exists() } match {
					case Some(folder) => Some(scala.io.Source.fromFile(new File(s"$folder$name.json")).mkString)
					case None=>
            logger.log(Level.SEVERE,s"could not find instructions file named $name.json")
						None
				}
		}


}
