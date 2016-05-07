package woshilaiceshide.sserver

import akka.util.ByteString
import spray.util.SingletonException
import spray.http._

package object http {

  type Parser = ByteString ⇒ Result

  sealed trait Result
  object Result {
    final case class NeedMoreData(next: Parser) extends Result
    final case class Emit(part: HttpMessagePart, closeAfterResponseCompletion: Boolean, continue: () ⇒ Result) extends Result
    //no lazy evaluation. this optimization is proved by facts.
    final case class EmitDirectly(part: HttpMessagePart, closeAfterResponseCompletion: Boolean, continue: Result) extends Result
    final case class Expect100Continue(continue: () ⇒ Result) extends Result
    final case class ParsingError(status: StatusCode, info: ErrorInfo) extends Result
    case object IgnoreAllFurtherInput extends Result with Parser { def apply(data: ByteString) = this }
  }

  class ParsingException(val status: StatusCode, val info: ErrorInfo) extends RuntimeException(info.formatPretty) {
    def this(status: StatusCode, summary: String = "") =
      this(status, ErrorInfo(if (summary.isEmpty) status.defaultMessage else summary))
    def this(summary: String) =
      this(StatusCodes.BadRequest, ErrorInfo(summary))
  }

  object NotEnoughDataException extends SingletonException

}