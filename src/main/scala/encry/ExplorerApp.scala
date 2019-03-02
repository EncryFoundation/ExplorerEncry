package encry

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import encry.settings.ExplorerSettings
import encry.parser._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import encry.database.{DBActor, DBService}

import scala.concurrent.ExecutionContextExecutor

object ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  val dbActor = system.actorOf(Props(new DBActor(settings.databaseSettings)), s"dbActor")
  system.actorOf(Props(new ParsersController(settings.parseSettings, dbActor)), s"parserController")

//  settings.parseSettings.nodes.foreach(node =>
//    system.actorOf(Props(new NodeParser(node)), s"ParserFor${node.getHostName}")
//  )
}
