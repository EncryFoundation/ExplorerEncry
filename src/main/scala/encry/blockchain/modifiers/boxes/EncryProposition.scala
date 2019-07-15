package encry.blockchain.modifiers.boxes

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.modifiers.mempool.transaction.EncryAddress.Address
import org.encryfoundation.common.modifiers.mempool.transaction._
import org.encryfoundation.common.utils.Algos
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.signatures.PublicKey

case class EncryProposition(contractHash: ContractHash)

object EncryProposition {

  case object UnlockFailedException extends Exception("Unlock failed")

  implicit val jsonDecoder: Decoder[EncryProposition] = (c: HCursor) =>
    for { contractHash <- c.downField("contractHash").as[String] }
      yield EncryProposition(Algos.decode(contractHash).getOrElse(Array.emptyByteArray))

  implicit val jsonEncoder: Encoder[EncryProposition] = (p: EncryProposition) => Map(
    "contractHash" -> Algos.encode(p.contractHash).asJson
  ).asJson

  def open: EncryProposition = EncryProposition(OpenContract.contract.hash)
  def pubKeyLocked(pubKey: PublicKey): EncryProposition = EncryProposition(PubKeyLockedContract(pubKey).contract.hash)
  def addressLocked(address: Address): EncryProposition = EncryAddress.resolveAddress(address).map {
    case p2pk: Pay2PubKeyAddress => EncryProposition(PubKeyLockedContract(p2pk.pubKey).contract.hash)
    case p2sh: Pay2ContractHashAddress => EncryProposition(p2sh.contractHash)
  }.getOrElse(throw EncryAddress.InvalidAddressException)
}
