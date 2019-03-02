package encry.database.data

import java.net.InetSocketAddress

import encry.blockchain.nodeRoutes.InfoRoute

case class Node(ip: String,
                status: Boolean,
                lastFullBlock: String,
                lastFullHeight: Int)

object Node {

  def apply(addr: InetSocketAddress, nodeInfo: InfoRoute): Node = Node(
    addr.getAddress.getHostAddress,
    status = true,
    nodeInfo.bestFullHeaderId,
    nodeInfo.fullHeight
  )

  def empty(addr: InetSocketAddress) = Node(addr.getAddress.getHostName, status = true, "", -1)
}