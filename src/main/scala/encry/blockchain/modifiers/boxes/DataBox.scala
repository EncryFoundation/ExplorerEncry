package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.core.Types
import org.encryfoundation.prismlang.core.wrapped.{PObject, PValue}

case class DataBox(override val proposition: EncryProposition,
                   override val nonce: Long,
                   data: Array[Byte]) extends EncryBox[EncryProposition] {

  override val typeId: Byte = DataBox.TypeId

  override val tpe: Types.Product = Types.DataBox

  override def asVal: PValue = PValue(asPrism, Types.DataBox)

  override def asPrism: PObject =
    PObject(baseFields ++ Map(
      "data" -> PValue(data, Types.PCollection.ofByte)
    ), tpe)

  override def toDBBoxes: DBBoxGeneralizedClass =
    DBBoxGeneralizedClass(Algos.encode(id), DataBox.TypeId, "", Algos.encode(proposition.contractHash), Algos.encode(data), nonce)
}

object DataBox {

  val TypeId: Byte = 4.toByte

  implicit val jsonDecoder: Decoder[DataBox] = (c: HCursor) => {
    for {
      proposition   <- c.downField("proposition").as[EncryProposition]
      nonce         <- c.downField("nonce").as[Long]
      data          <- c.downField("data").as[Array[Byte]]
    } yield DataBox(
      proposition,
      nonce,
      data
    )
  }

  implicit val jsonEncoder: Encoder[DataBox] = (bx: DataBox) => Map(
    "type" -> TypeId.asJson,
    "id" -> Algos.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "data" -> Algos.encode(bx.data).asJson,
  ).asJson
}
