package encry.database.data

import java.net.InetSocketAddress

case class InputToNode(bxIdInInput: String, node: String)

object InputToNode {

  def apply(input: DBInput, addr: InetSocketAddress): InputToNode = new InputToNode(input.bxId, addr.getAddress.getHostAddress)

  def apply(bxIdInInput: String, node: String): InputToNode = new InputToNode(bxIdInInput, node)
}
