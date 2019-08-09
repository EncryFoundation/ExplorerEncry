package encry.settings

import java.net.InetSocketAddress
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader

case class ExplorerSettings(parseSettings: ParseSettings,
                            blackListSettings: BlackListSettings,
                            databaseSettings: DatabaseSettings,
                            nodeSettings: NodeSettings)

object ExplorerSettings {

  implicit val inetSocketAddressReader: ValueReader[InetSocketAddress] = { (config: Config, path: String) =>
    val split = config.getString(path).split(":")
    new InetSocketAddress(split(0), split(1).toInt)
  }

  val configPath: String = "explorer"

  def read: ExplorerSettings = ConfigFactory.load("local.conf")
    .withFallback(ConfigFactory.load()).as[ExplorerSettings](configPath)
}
