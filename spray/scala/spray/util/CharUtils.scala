package spray.util

import scala.collection.immutable.NumericRange
import akka.util.ByteString
import scala.annotation.tailrec
import java.lang.{ StringBuilder ⇒ JStringBuilder }

// TODO: replace with spray.http.parser.CharMask
object CharUtils {
  // compile time constants
  private final val LOWER_ALPHA = 0x01
  private final val UPPER_ALPHA = 0x02
  private final val ALPHA = LOWER_ALPHA | UPPER_ALPHA
  private final val DIGIT = 0x04
  private final val HEX_LETTER = 0x08
  private final val HEX_DIGIT = DIGIT | HEX_LETTER
  private final val TOKEN_SPECIALS = 0x10
  private final val TOKEN = ALPHA | DIGIT | TOKEN_SPECIALS
  private final val WSP = 0x20
  private final val NEWLINE = 0x40

  private[this] val props = new Array[Byte](128)

  private def is(c: Int, mask: Int): Boolean = (props(index(c)) & mask) != 0
  private def index(c: Int): Int = c & ((c - 127) >> 31) // branchless for `if (c <= 127) c else 0`
  private def mark(mask: Int, chars: Char*): Unit = chars.foreach(c ⇒ props(index(c)) = (props(index(c)) | mask).toByte)
  private def mark(mask: Int, range: NumericRange[Char]): Unit = mark(mask, range.toSeq: _*)

  mark(LOWER_ALPHA, 'a' to 'z')
  mark(UPPER_ALPHA, 'A' to 'Z')
  mark(DIGIT, '0' to '9')
  mark(HEX_LETTER, 'a' to 'f')
  mark(HEX_LETTER, 'A' to 'F')
  mark(TOKEN_SPECIALS, '!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~')
  mark(WSP, ' ', '\t')
  mark(NEWLINE, '\r', '\t')

  def isTokenChar(c: Char) = is(c, TOKEN)
  def isDigit(c: Char) = is(c, DIGIT)
  def isWhitespace(c: Char) = is(c, WSP)
  def isWhitespaceOrNewline(c: Char) = is(c, WSP | NEWLINE)
  def isHexDigit(c: Char) = is(c, HEX_DIGIT)

  def hexValue(c: Char): Int = (c & 0x1f) + ((c >> 6) * 0x19) - 0x10

  def toLowerCase(c: Char): Char = if (is(c, UPPER_ALPHA)) (c + 0x20).toChar else c
  def abs(i: Int): Int = { val j = i >> 31; (i ^ j) - j }
  def escape(c: Char): String = c match {
    case '\t' ⇒ "\\t"
    case '\r' ⇒ "\\r"
    case '\n' ⇒ "\\n"
    case x if Character.isISOControl(x) ⇒ "\\u%04x" format c.toInt
    case x ⇒ x.toString
  }

  def asciiString(input: ByteString, start: Int, end: Int): String = {
    @tailrec def build(ix: Int = start, sb: JStringBuilder = new JStringBuilder(end - start)): String =
      if (ix == end) sb.toString else build(ix + 1, sb.append(input(ix).toChar))
    if (start == end) "" else build()
  }

  def lowerHexDigit(long: Long): Char = lowerHexDigit_internal((long & 0x0FL).toInt)
  def lowerHexDigit(int: Int): Char = lowerHexDigit_internal(int & 0x0F)
  private def lowerHexDigit_internal(i: Int) = (48 + i + (39 & ((9 - i) >> 31))).toChar

  def upperHexDigit(long: Long): Char = upperHexDigit_internal((long & 0x0FL).toInt)
  def upperHexDigit(int: Int): Char = upperHexDigit_internal(int & 0x0F)
  private def upperHexDigit_internal(i: Int) = (48 + i + (7 & ((9 - i) >> 31))).toChar

}
