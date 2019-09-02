package encry.database

import java.net.InetSocketAddress

import com.typesafe.scalalogging.StrictLogging
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.hikari.HikariTransactor
import encry.blockchain.nodeRoutes.InfoRoute

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import Queries._
import encry.blockchain.modifiers.{Block, Header}

case class DBService(pgTransactor: HikariTransactor[IO]) extends StrictLogging {

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

  def insertBlockFromNode(block: Block, nodeAddr: InetSocketAddress, nodeInfo: InfoRoute): Future[Unit] =
    runAsync(processBlock(block, nodeAddr, nodeInfo), "blockInsert")

  def deleteBlock(addr: InetSocketAddress, block: Block): Future[Unit] =
    runAsync(removeBlock(addr, block), "deleteBlock")

  def blocksIds(from: Int, to: Int): Future[List[String]] = runAsync(blocksIdsQuery(from, to), "blocksIds")

  private def runAsync[A](io: ConnectionIO[A], queryName: String): Future[A] = {
    val start = System.nanoTime()
    (for { res <- io.transact(pgTransactor) } yield res)
      .unsafeToFuture()
      .map { result =>
        logger.debug(s"Query $queryName took ${(System.nanoTime() - start).toDouble / 1000000000} seconds to perform")
        result
      }.recoverWith {
        case NonFatal(th) =>
          logger.warn(s"Failed to perform $queryName", th)
          Future.failed(th)
      }
  }

}
