package encry.blockchain.modifiers

import encry.blockchain.modifiers.Directive.DTypeId
import encry.blockchain.modifiers.boxes.EncryBaseBox
import io.circe.{Decoder, DecodingFailure, Encoder}
import org.encryfoundation.common.transaction.EncryAddress.Address
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.hash.Digest32

trait Directive {

  val isValid: Boolean
  val typeId: DTypeId

  def toDBDirective: DBDirectiveGeneralizedClass

  def boxes(digest: Digest32, idx: Int): EncryBaseBox

}

object Directive {

  type DTypeId = Byte

  implicit val jsonDecoder: Decoder[Directive] = {
    Decoder.instance { c =>
      c.downField("typeId").as[DTypeId] match {
        case Right(s) => s match {
          case TransferDirective.TypeId => TransferDirective.jsonDecoder(c)
          case AssetIssuingDirective.TypeId => AssetIssuingDirective.jsonDecoder(c)
          case ScriptedAssetDirective.TypeId => ScriptedAssetDirective.jsonDecoder(c)
          case DataDirective.TypeId => DataDirective.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect directive typeID", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}

case class DirectiveDBVersion(txId: String,
                              numberInTx: Int,
                              dTypeId: DTypeId,
                              isValid: Boolean,
                              contractHash: String,
                              amount: Long,
                              address: String,
                              tokenIdOpt: Option[String],
                              data: String)

trait DBDirective {

}

case class DBTransferDirective(address: Address, tokenIdOpt: Option[ADKey] = None) extends DBDirective

case class DBDataDirective(contractHash: ContractHash, data: Array[Byte]) extends DBDirective

case class DBAssetIssuingDirective(contractHash: ContractHash, amount: Long) extends DBDirective

case class DBDirectiveGeneralizedClass(address: Address = "", amount: Long = 0L)