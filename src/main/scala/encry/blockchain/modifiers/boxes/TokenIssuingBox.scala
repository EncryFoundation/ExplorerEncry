package encry.blockchain.modifiers.boxes

import encry.blockchain.modifiers.boxes.Box.Amount
import encry.blockchain.modifiers.boxes.TokenIssuingBox.TokenId
import io.circe.{Decoder, HCursor}
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.core.Types
import org.encryfoundation.prismlang.core.wrapped.{PObject, PValue}

case class TokenIssuingBox(override val proposition: EncryProposition,
                           override val nonce: Long,
                           override val amount: Amount,
                           tokenId: TokenId)
  extends EncryBox[EncryProposition] with MonetaryBox {

  override val typeId: Byte = TokenIssuingBox.TypeId

  override val tpe: Types.Product = Types.AssetIssuingBox

  override def asVal: PValue = PValue(asPrism, Types.DataBox)

  override def asPrism: PObject =
    PObject(baseFields ++ Map(
      "amount" -> PValue(amount, Types.PInt)
    ), tpe)

  override def toDBBoxes: DBBoxGeneralizedClass =
    DBBoxGeneralizedClass(Algos.encode(id),Algos.encode(tokenId),Algos.encode(proposition.contractHash),nonce = nonce)
}

object TokenIssuingBox {

  type TokenId = Array[Byte]

  val TypeId: Byte = 3.toByte

  implicit val jsonDecoder: Decoder[TokenIssuingBox] = (c: HCursor) => {
    for {
      proposition   <- c.downField("proposition").as[EncryProposition]
      nonce         <- c.downField("nonce").as[Long]
      amount        <- c.downField("amount").as[Long]
      tokenId       <- c.downField("tokenId").as[String]
    } yield TokenIssuingBox(
      proposition,
      nonce,
      amount,
      Algos.decode(tokenId).getOrElse(Array.emptyByteArray)
    )
  }

}
