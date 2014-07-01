package io.legs.utils

import redis.{RedisCommands, RedisServer, RedisClientPool}
import scala.concurrent.{Future, Awaitable, Await}
import scala.concurrent.duration.Duration
import java.util.logging.{Level, Logger}

object RedisProvider {

	private lazy val logger = Logger.getLogger(this.getClass.getSimpleName)

	implicit val akkaSystem = akka.actor.ActorSystem("RedisProviderActors")
	implicit val ec = akkaSystem.dispatcher

	val poolSize = 40

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
				logger.log(Level.SEVERE,s"dropping datatabase! $host/$port/$db")
				blocking {
					_.flushdb()
				}
			case _ =>
        logger.log(Level.SEVERE,s"database drop requested, but validator $validator was wrong! $host:$port/$db")
		}

}