package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cats.effect.{Blocker, ExitCode, IO, IOApp}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import encry.database.DBService
import encry.settings.ExplorerSettings

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object  ExplorerApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val settings = ExplorerSettings.read

    //implicit val cs = IO.contextShift(ExecutionContext.global)

    val pgTransactor = for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
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
      val dbService = DBService(settings.databaseSettings, xa)
      IO.pure(system.actorOf(Props(new ParsersController(settings.parseSettings, settings.blackListSettings, dbService)), s"parserController"))
    }.map(_ => ExitCode.Success)
  }
}
