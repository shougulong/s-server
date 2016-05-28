package woshilaiceshide.sserver.http

import java.nio._
import spray.http._

trait RichBytesRendering extends Rendering {

  def reset(): Unit
  def to_byte_buffer(): ByteBuffer

  protected var original_start: Int = 0
  def set_original_start(i: Int) = this.original_start = i

}

private[http] final class RevisedByteArrayRendering(sizeHint: Int) extends ByteArrayRendering(sizeHint) with RichBytesRendering {

  override def reset() = {
    this.size = original_start
    buffer.clear()
  }

  private val buffer = ByteBuffer.wrap(this.array)

  def to_byte_buffer(): ByteBuffer = {
    buffer.limit(this.size)
    buffer
  }
}

private[http] final class ByteBufferRendering(sizeHint: Int) extends Rendering with RichBytesRendering {

  protected var original_buffer = ByteBuffer.allocateDirect(sizeHint)
  protected var buffer = original_buffer

  protected var flipped = false

  def get: Array[Byte] = {
    if (!flipped) {
      flipped = true
      buffer.flip()
    }
    buffer.mark()
    val bytes = new Array[Byte](buffer.remaining())
    buffer.get(bytes)
    buffer.reset()
    bytes
  }

  def reset(): Unit = {
    buffer = original_buffer
    original_buffer.clear()
    original_buffer.position(original_start)
  }

  def to_byte_buffer(): ByteBuffer = {
    buffer
  }

  def ~~(char: Char): this.type = {
    val oldSize = growBy(1)
    buffer.put(char.toByte)
    this
  }

  def ~~(bytes: Array[Byte]): this.type = {
    if (bytes.length > 0) {
      val oldSize = growBy(bytes.length)
      buffer.put(bytes, 0, bytes.length)
    }
    this
  }

  @scala.annotation.tailrec
  private def ~~(head: HttpData, tail: List[HttpData.NonEmpty]): Unit = {

    head match {
      case HttpData.Empty => {
        if (!tail.isEmpty) {
          ~~(tail.head, tail.tail)
        }
      }
      case HttpData.Bytes(bytes) => {
        bytes.copyToBuffer(buffer)
        if (!tail.isEmpty) {
          ~~(tail.head, tail.tail)
        }
      }
      case HttpData.FileBytes(fileName, offset, length) => {
        val file = new java.io.FileInputStream(fileName)
        try {
          val channel = file.getChannel()
          channel.position(offset)
          channel.read(buffer)
        } finally {
          file.close()
        }
        if (!tail.isEmpty) {
          ~~(tail.head, tail.tail)
        }
      }
      case HttpData.Compound(head1: HttpData.SimpleNonEmpty, tail1: HttpData.NonEmpty) => {
        ~~(head1, tail1 :: tail)
      }

    }

  }

  def ~~(data: HttpData): this.type = {
    if (data.nonEmpty) {
      if (data.length <= Int.MaxValue) {
        growBy(data.length.toInt)
        //data.copyToArray(array, targetOffset = oldSize)

        data match {
          case HttpData.Bytes(bytes) => {
            bytes.copyToBuffer(buffer)
          }
          case HttpData.FileBytes(fileName, offset, length) => {
            val file = new java.io.FileInputStream(fileName)
            try {
              val channel = file.getChannel()
              channel.position(offset)
              channel.read(buffer)
            } finally file.close()
          }
          case HttpData.Compound(head: HttpData.SimpleNonEmpty, tail: HttpData.NonEmpty) => {
            this ~~ (head, List(tail))
          }
          case HttpData.Empty => {}

        }

      } else sys.error("Cannot create byte array greater than 2GB in size")
    }
    this
  }

  private def growBy(delta: Int): Unit = {

    if (delta > buffer.remaining()) {
      val needed = buffer.capacity() + delta - buffer.remaining()
      if (needed < Int.MaxValue) {

        val tmp = ByteBuffer.allocate(needed)
        buffer.flip()
        tmp.put(buffer)
        buffer = tmp

      } else sys.error("Cannot create byte array greater than 2GB in size")
    }

  }
}