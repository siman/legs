package io.legs.utils

import redis.{RedisCommands, RedisServer, RedisClientPool}
import scala.concurrent.{Future, Awaitable, Await}
import scala.concurrent.duration.Duration
import grizzled.slf4j.Logger

object RedisProvider {

	private lazy val logger = Logger(getClass)

	implicit val akkaSystem = akka.actor.ActorSystem("RedisProviderActors")
	implicit val ec = akkaSystem.dispatcher

	val poolSize = Config.getParam("redis.pool-size").get.toInt

	private def host = Config.getParam("redis.host").get
	private def port = Config.getParam("redis.port").get.toInt
	private def db = Config.getParam("redis.db").get.toInt

	lazy val redisPool = {
		logger.info(s"initializing pool $host:$port/$db")
		RedisClientPool((0 until poolSize).map(_=>RedisServer(host,port,None,Some(db))),Config.Env.toString())
	}

	def blocking[T](body: RedisCommands => Awaitable[T] ) : T =
		Await.result(body(redisPool), Duration(2L, "seconds"))

	def blockingList[T](body: RedisCommands => List[Future[T]]) : List[T] =
		Await.result(Future.sequence(body(redisPool)),Duration(10L,"seconds"))


	def drop(validator : String) =
		validator match {
			case "!!!" =>
				logger.error(s"dropping datatabase! $host/$port/$db")
				blocking {
					_.flushdb()
				}
			case _ =>
				logger.error(s"database drop requested, but validator $validator was wrong! $host:$port/$db")
		}

}