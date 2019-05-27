package encry.parser

import java.net.InetSocketAddress
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.StrictLogging
import encry.blockchain.modifiers.{Block, Header}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.{ActivateNodeAndGetNodeInfo, MyCase}
import encry.parser.NodeParser.{PeersList, PingNode, SetNodeParams}
import encry.settings.ParseSettings


class SimpleNodeParser(node: InetSocketAddress,
                       parserController: ActorRef,
                       dbActor: ActorRef,
                       settings: ParseSettings) extends Actor with StrictLogging{

  import context.dispatcher
  import scala.concurrent.duration._

  val parserRequests: ParserRequests = ParserRequests(node)
  var currentNodeInfo: InfoRoute = InfoRoute.empty
  var currentNodeBestBlock: Block = Block.empty
  var currentNodeBestBlockId: String = ""
  var currentBestBlockHeight: AtomicInteger = new AtomicInteger(-1)

  override def preStart(): Unit = {
//    logger.info(s"Start monitoring: ${node.getAddress}  444")
    context.system.scheduler.schedule(
      10 seconds,
      10 seconds
    )(self ! PingNode)
  }

  override def receive: Receive = workingCycle

  def workingCycle: Receive = {
    case PingNode =>
      parserRequests.getInfo match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(infoRoute) =>
  //        logger.info(s"Get node info on $node during prepare status")
          currentNodeInfo = infoRoute
          dbActor ! MyCase(node, infoRoute)
      }
      parserRequests.getPeers match {
        case Left(err) => logger.info(s"Error during request to $node: ${err.getMessage}")
        case Right(peersList) =>
          parserController ! PeersList(peersList.collect {
            case peer  => {
             // println(s"${peer.address.getAddress}  111")
              peer.address.getAddress}
          })
//          logger.info(s"Send peer list: ${
//            peersList.collect {
//              case peer => peer.address.getAddress
//            }
//          } to parserController.")
      }
  }

}
