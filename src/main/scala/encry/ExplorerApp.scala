package encry

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import encry.settings.ExplorerSettings
import encry.database.DBActor
import encry.database.DBActor.DropBlocksFromNode
import encry.parser.ParserRequests

import scala.concurrent.ExecutionContextExecutor

object  ExplorerApp extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val settings = ExplorerSettings.read

  val dbActor = system.actorOf(Props(new DBActor(settings.databaseSettings)), s"dbActor")
//  system.actorOf(Props(new ParsersController(settings.parseSettings, dbActor)), s"parserController")
  val addr = settings.parseSettings.nodes.head
  val reqParser = ParserRequests(addr)
  val possibleBlock = reqParser.getBlock("017f9afbc924659b3138c896a17da421e342f1dfdb6917312045dc7ad82b1cfb")
  println(possibleBlock)
  possibleBlock.map(block => dbActor ! DropBlocksFromNode(addr, List(block)))
}
