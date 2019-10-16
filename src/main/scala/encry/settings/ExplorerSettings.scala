package encry.settings

import java.net.InetSocketAddress

import com.typesafe.config.{Config, ConfigFactory}
import encry.network.NetworkTimeProviderSettings
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader

import scala.concurrent.duration.FiniteDuration

case class ExplorerSettings(parseSettings: ParseSettings,
                            blackListSettings: BlackListSettings,
                            databaseSettings: DatabaseSettings,
                            ntpSettings: NetworkTimeProviderSettings,
                            networkSettings: NetworkSettings,
                            multisigSettings: MultisigSettings)

case class NetworkSettings(syncPacketLength: Int,
                           bindAddressHost: String,
                           bindAddressPort: Int,
                           nodeName: String,
                           appVersion: String,
                           handshakeTimeout: FiniteDuration,
                           peerForConnectionHost: String,
                           peerForConnectionPort: Int,
                           peerForConnectionApiPort: Int,
                           declaredAddressHost: String,
                           declaredAddressPort: Int)

case class MultisigSettings(checkTxMinedPeriod: Int, numberOfBlocksToCheck: Int, mnemonicKeys: List[String])

case class ParseSettings(nodes: List[InetSocketAddress], recoverBatchSize: Int, infinitePing: Boolean, askNode: Boolean,
                         numberOfAttempts: Option[Int] = None)

case class DatabaseSettings(host: String, user: String, password: String, maxPoolSize: Int, connectionTimeout: Long)

case class BlackListSettings(banTime: FiniteDuration, cleanupTime: FiniteDuration)

object ExplorerSettings {

  implicit val inetSocketAddressReader: ValueReader[InetSocketAddress] = { (config: Config, path: String) =>
    val split = config.getString(path).split(":")
    new InetSocketAddress(split(0), split(1).toInt)
  }

  def read: ExplorerSettings = ConfigFactory.load("local.conf")
    .withFallback(ConfigFactory.load()).as[ExplorerSettings]("explorer")
}
