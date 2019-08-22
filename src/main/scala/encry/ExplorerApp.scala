package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import encry.settings.ExplorerSettings
import encry.database.DBActor

import scala.concurrent.ExecutionContextExecutor

object  ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  val dbActor = system.actorOf(Props(new DBActor(settings.databaseSettings)), s"dbActor")
  system.actorOf(Props(new ParsersController(settings.parseSettings, settings.blackListSettings, dbActor)), s"parserController")
}
