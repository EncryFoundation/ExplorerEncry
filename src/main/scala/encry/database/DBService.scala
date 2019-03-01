package encry.database

import com.typesafe.scalalogging.StrictLogging
import encry.settings.DatabaseSettings
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.implicits._
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.hikari.HikariTransactor
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global

case class DBService(settings: DatabaseSettings) extends StrictLogging {

  private lazy val dataSource = new HikariDataSource

  dataSource.setJdbcUrl(settings.host + "?loggerLevel=OFF")
  dataSource.setUsername(settings.user)
  dataSource.setPassword(settings.password)
  dataSource.setMaximumPoolSize(settings.maxPoolSize)

  private lazy val pgTransactor: HikariTransactor[IO] = HikariTransactor[IO](dataSource)

  def shutdown(): Future[Unit] = {
    logger.info("Shutting down dbService")
    pgTransactor.shutdown.unsafeToFuture
  }

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
