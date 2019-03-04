package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, DecodingFailure, Encoder}

trait EncryBaseBox {

  val id: String

  val typeId: Byte

  val proposition: String

  val nonce: Long
}

object EncryBaseBox {

  implicit val jsonDecoder: Decoder[EncryBaseBox] = {
    Decoder.instance { c =>
      c.downField("type").as[Byte] match {
        case Right(s) => s match {
          case AssetBox.TypeId        => AssetBox.jsonDecoder(c)
          case DataBox.TypeId         => DataBox.jsonDecoder(c)
          case TokenIssuingBox.TypeId => TokenIssuingBox.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}