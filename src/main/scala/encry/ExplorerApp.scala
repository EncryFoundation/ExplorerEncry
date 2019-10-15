package encry

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import cats.effect.{Blocker, ContextShift, IO}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.net.network.NetworkServer
import encry.net.utils.NetworkTimeProvider
import encry.settings.ExplorerSettings

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val pgTransactor = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](settings.databaseSettings.maxPoolSize)
    te <- ExecutionContexts.cachedThreadPool[IO]
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      settings.databaseSettings.host,
      settings.databaseSettings.user,
      settings.databaseSettings.password,
      ce,
      Blocker.liftExecutionContext(te)
    )
  } yield xa

  pgTransactor.use { xa =>
    xa.configure { ds =>
      IO {
        ds.setMaximumPoolSize(settings.databaseSettings.maxPoolSize)
        ds.setConnectionTimeout(settings.databaseSettings.connectionTimeout)
      }
    } *> IO {
      val timeProvider: NetworkTimeProvider = new NetworkTimeProvider(settings.ntpSettings)
      val networkServer: ActorRef = system.actorOf(NetworkServer.props(settings.networkSettings, timeProvider), "networkServer")

//      val dbService = DBService(xa)
//      val dbActor = system.actorOf(Props(new DBActor(dbService)), s"dbActor")
//      system.actorOf(Props(new ParsersController(settings.parseSettings, settings.blackListSettings, dbActor)), s"parserController")
    } *> IO.never
  }.unsafeRunSync()
}
