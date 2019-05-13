package encry.database

import java.net.InetSocketAddress
import com.typesafe.scalalogging.StrictLogging
import encry.settings.DatabaseSettings
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.implicits._
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.hikari.HikariTransactor
import encry.blockchain.nodeRoutes.InfoRoute
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import Queries._
import encry.blockchain.modifiers.Block
import encry.database.data.Node

case class DBService(settings: DatabaseSettings) extends StrictLogging {

  private lazy val dataSource = new HikariDataSource

  dataSource.setJdbcUrl(settings.host)
  dataSource.setUsername(settings.user)
  dataSource.setPassword(settings.password)
  dataSource.setMaximumPoolSize(settings.maxPoolSize)

  private lazy val pgTransactor: HikariTransactor[IO] = HikariTransactor[IO](dataSource)

  def shutdown(): Future[Unit] = {
    logger.info("Shutting down dbService")
    pgTransactor.shutdown.unsafeToFuture
  }

  def getNodeInfo(addr: InetSocketAddress): Future[Option[Node]] = {
    runAsync(nodeInfoQuery(addr), "nodeInfo")
  }

  def deleteBlocksFromNode(addr: InetSocketAddress, headerId: String): Future[Int] = {
    runAsync(dropHeaderFromNode(headerId, addr), "deleteBlocks")
  }

  def activateNode(addr: InetSocketAddress, infoRoute: InfoRoute): Future[Int] =
    runAsync(insertNode(addr, infoRoute), "activateNode")

  def activateOrGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute): Future[Option[Node]] = {
    val res = Await.result(getNodeInfo(addr), 3.minutes)
    res match {
      case Some(info) => Future.successful(Some(info))
      case None => activateNode(addr, InfoRoute.empty)
        Future.successful(Some(Node.empty(addr)))
    }
  }

  def insertBlockFromNode(block: Block, nodeAddr: InetSocketAddress): Future[Int] =
    runAsync(proccessBlock(block, nodeAddr), "blockInsert")

  private def runAsync[A](io: ConnectionIO[A], queryName: String): Future[A] =
    (for {
      res <- io.transact(pgTransactor)
    } yield res)
      .unsafeToFuture()
      .recoverWith {
        case NonFatal(th) =>
          logger.warn(s"Failed to perform $queryName query with exception ${th.getLocalizedMessage}")
          Future.failed(th)
      }
}
