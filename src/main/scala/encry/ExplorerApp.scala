package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cats.effect.{Blocker, IO}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.database.{DBActor, DBService}
import encry.settings.ExplorerSettings
import doobie.implicits._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object  ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  implicit val cs = IO.contextShift(ExecutionContext.global)

  val pgTransactor = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](20)
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
    //PrepareQueries.prepareHeaderInsertQuery.transact(xa).orElse(IO.unit) *>
    xa.configure { ds =>
      IO {
        ds.setMaximumPoolSize(20)
        ds.setConnectionTimeout(60000)
      }
    } *> IO {
      val dbService = DBService(settings.databaseSettings, xa)
      val dbActor = system.actorOf(Props(new DBActor(settings.databaseSettings, dbService)), s"dbActor")
      system.actorOf(Props(new ParsersController(settings.parseSettings, settings.blackListSettings, dbActor)), s"parserController")
    } *> IO.never
  }.unsafeRunSync()
}
