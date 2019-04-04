package encry.blockchain.modifiers.boxes

import io.circe.{Decoder, DecodingFailure}

trait EncryBaseBoxAPI {

  val id: String

  val typeId: Byte

  val proposition: String

  val nonce: Long
}

object EncryBaseBoxAPI {

  implicit val jsonDecoder: Decoder[EncryBaseBoxAPI] = {
    Decoder.instance { c =>
      c.downField("type").as[Byte] match {
        case Right(s) => s match {
          case AssetBoxAPIAPI.TypeId        => AssetBoxAPIAPI.jsonDecoder(c)
          case DataBoxAPI.TypeId         => DataBoxAPI.jsonDecoder(c)
          case TokenIssuingBoxAPI.TypeId => TokenIssuingBoxAPI.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}