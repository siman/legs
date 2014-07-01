package io.legs.utils

import com.typesafe.config.ConfigFactory
import java.io.File


object Config {

	object Env extends Enumeration {
		type Env = Value
		val DEV, PRODUCTION,TEST = Value
	}

	import Env._

	private var env = Env.DEV

	def setEnv(newEnv : Env.Value) = env = newEnv

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
