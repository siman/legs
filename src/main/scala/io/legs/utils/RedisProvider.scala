package io.legs.utils

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import redis.{RedisClientPool, RedisCommands, RedisServer}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object RedisProvider {

	private lazy val logger = Logger(LoggerFactory.getLogger(getClass))

	implicit val akkaSystem = akka.actor.ActorSystem("RedisProviderActors")
	implicit val ec = akkaSystem.dispatcher

	private lazy val poolSize = Config.getParam("redis.pool-size").get.toInt
	private lazy val host = Config.getParam("redis.host").get
	private lazy val port = Config.getParam("redis.port").get.toInt
	private lazy val db = Config.getParam("redis.db").get.toInt

	logger.info(s"setting up redis with host:$host port:$port db:$db poolSize:$poolSize")

	private lazy val redisPool = {
		logger.info(s"initializing pool $host:$port/$db")
		RedisClientPool(
			(0 until poolSize)
				.map( _ => RedisServer(host,port,None,Some(db))),Config.Env.toString() )
	}

	def asyncRedis[T](body: RedisCommands => Future[T]) : Future[T] = body(redisPool)

	def blockingRedis[T](body: RedisCommands => Future[T])(implicit ec : ExecutionContext) : T =
		Await.result(asyncRedis(body),Duration("10 seconds"))

	def drop(validator : String) : Unit =
		validator match {
			case "!!!" if Config.env == Config.Env.TEST =>
				logger.warn(s"dropping datatabase! $host/$port/$db")
				Await.result(redisPool.flushdb(), Duration(2L, "seconds"))
			case _ =>
				logger.error(s"database drop requested, but validator $validator was wrong! $host:$port/$db")
		}

}