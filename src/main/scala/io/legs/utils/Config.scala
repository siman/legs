package io.legs.utils

import com.typesafe.config.ConfigFactory
import java.io.File

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory


object Config {

	private lazy val log = Logger(LoggerFactory.getLogger(getClass))

	object Env extends Enumeration {
		type Env = Value
		val DEV, PRODUCTION,TEST = Value
	}

	import Env._

	var env =
		if (null != System.getProperty("isTest")) Env.TEST
		else
			if (System.getenv("LEGS_UI_ENV") == "production") Env.PRODUCTION
			else Env.DEV

	log.info("initial config env:" + env.toString)

	def setEnv(newEnv : Env.Value) = env = {
		log.info("changing config env to:" + newEnv.toString)
		newEnv
	}

	def getParam(name: String) : Option[String] =
		if (getConfig.hasPath(getPrefix + "." + name)) Some(getConfig.getString(getPrefix + "." + name))
		else None

	lazy val getConfig = ConfigFactory.load()

	private def getPrefix =
		env match {
			case TEST => "test"
			case DEV => "dev"
			case PRODUCTION => "production"
		}

}
