package encry.database

import java.net.InetSocketAddress

import com.typesafe.scalalogging.StrictLogging
import encry.settings.DatabaseSettings
import cats.effect.{Blocker, IO}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.hikari.HikariTransactor
import doobie.postgres.implicits._
import encry.blockchain.nodeRoutes.InfoRoute
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import Queries._
import encry.blockchain.modifiers.{Block, Header}

case class DBService(settings: DatabaseSettings, transactor: HikariTransactor[IO]) extends StrictLogging {

  def getNodeInfo(addr: InetSocketAddress): Future[Option[Header]] = {
    runAsync(nodeInfoQuery(addr), "nodeInfo")
  }

  def activateNode(addr: InetSocketAddress, infoRoute: InfoRoute, status: Boolean): Future[Int] =
    runAsync(insertNode(addr, infoRoute, status), "activateNode")

  def activateOrGetNodeInfo(addr: InetSocketAddress, infoRoute: InfoRoute): Future[Header] =
    getNodeInfo(addr).flatMap {
      case Some(info) => Future.successful(info)
      case None => activateNode(addr, InfoRoute.empty, status = false).map(_ => Header.empty)
    }


  def insertBlockFromNode(block: Block, nodeAddr: InetSocketAddress, nodeInfo: InfoRoute): Future[List[String]] =
    runAsync(processBlock(block, nodeAddr, nodeInfo), "blockInsert")

  def deleteBlock(addr: InetSocketAddress, block: Block): Future[Int] =
    runAsync(removeBlock(addr, block), "deleteBlock")

  def blocksIds(from: Int, to: Int): Future[List[String]] = runAsync(blocksIdsQuery(from, to), "blocksIds")

  private def runAsync[A](io: ConnectionIO[A], queryName: String): Future[A] = {
    (for {
      res <- io.transact(transactor)
    } yield res)
      .unsafeToFuture()
      .recoverWith {
      case NonFatal(th) =>
        logger.warn(s"Failed to perform $queryName query with exception ${th.getLocalizedMessage}")
        Future.failed(th)
    }
  }

}
