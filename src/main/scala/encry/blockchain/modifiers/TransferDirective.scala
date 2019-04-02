package encry.blockchain.modifiers

import io.circe.syntax._
import encry.blockchain.modifiers.Directive.DTypeId
import encry.database.data.{DBDirective, DBOutput}
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.common.transaction.EncryAddress
import org.encryfoundation.common.transaction.EncryAddress.Address
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import scorex.crypto.encode.Base16

case class TransferDirective(address: Address,
                             amount: Long,
                             tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val isValid: Boolean = amount > 0 && EncryAddress.resolveAddress(address).isSuccess

  override def toDbDirective(txId: String, numberInTx: Int): DBOutput =
    DBDirective(txId, numberInTx, typeId, isValid, "", amount, address, tokenIdOpt.map(Base16.encode), "")

  override val typeId: DTypeId = TransferDirective.TypeId
}

object TransferDirective {

  val TypeId: DTypeId = 1.toByte

  implicit val jsonEncoder: Encoder[TransferDirective] = (d: TransferDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Algos.encode(id)).getOrElse("null").asJson
  ).asJson

  implicit val jsonDecoder: Decoder[TransferDirective] = (c: HCursor) => {
    for {
      address <- c.downField("address").as[String]
      amount <- c.downField("amount").as[Long]
      tokenIdOpt <- c.downField("tokenId").as[Option[String]]
    } yield TransferDirective(
      address,
      amount,
      tokenIdOpt.flatMap(id => Algos.decode(id).map(ADKey @@ _).toOption)
    )
  }
}