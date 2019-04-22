package encry.parser.NodeParserTests

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import encry.blockchain.nodeRoutes.InfoRoute
import encry.database.DBActor.ActivateNodeAndGetNodeInfo
import encry.parser.NodeParser.{BlockFromNode, PingNode, SetNodeParams}
import encry.parser.{NodeParser, ParserRequests}
import encry.settings.ExplorerSettings
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, OneInstancePerTest, WordSpecLike}

class forkSpec extends WordSpecLike
with BeforeAndAfterAll
with Matchers
with OneInstancePerTest
with MockFactory{

  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "this test" should {
    val node = new InetSocketAddress("172.16.13.10", 9001)
    val parsersController: ActorRef = TestProbe().ref
    val dbActor: ActorRef = TestProbe().ref
    val settings: ExplorerSettings = ExplorerSettings.read
    val parserRequestsStab: ParserRequests = stub[ParserRequests]
//    val parserRequestsMock: ParserRequests = mock[ParserRequests]
   // val lastIds: List[String] = List("2", "3", "100")
    val isRecovering: AtomicBoolean = new AtomicBoolean(true)
    val nodeParser: TestActorRef[NodeParser] =
      TestActorRef[NodeParser](Props(new NodeParser(node, parsersController, dbActor, settings.parseSettings, parserRequestsStab)))


    "get new 100 blocks" in{
      nodeParser ! PingNode
      nodeParser ! SetNodeParams("123456", 200)
      nodeParser ! PingNode
      //parserRequestsStab.getInfo _ returns Right(List("1", "2", "3"))
      parserRequestsStab.getLastIds _ when(3, 50) returns Right(List("1", "2", "3"))
     // assert(nodeParser.underlyingActor.lastIds.)
      assert(nodeParser.underlyingActor.lastIds.size == 3 )
      //assert(parserRequestsStab.getLastIds(3, 100) = Right(List("1,2,3"))
    }
  }
}
