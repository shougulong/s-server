/*
 * Copyright © 2011-2015 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka

import java.nio._
import akka.util.ByteString

package object spray {

  def createByteStringUnsafe(bytes: Array[Byte]): ByteString.ByteString1C =
    ByteString.ByteString1C(bytes)

  def createByteStringUnsafe(bytes: Array[Byte], start: Int, len: Int): ByteString.ByteString1 =
    ByteString.ByteString1(bytes, start, len)

  def createByteStringUnsafe(buffer: ByteBuffer) = {
    akka.util.ByteString.ByteString1(buffer.array(), buffer.position(), buffer.limit())
  }

  def shadowTailOfByteStrings(bs: ByteString.ByteStrings): ByteString = {

    val v = bs.bytestrings
    val last = v.last

    if (last.length == bs.length) {
      val bytes = last.toArray
      createByteStringUnsafe(bytes, 0, bytes.length)
    } else {
      val bytes = last.toArray
      val tmp = createByteStringUnsafe(bytes, 0, bytes.length)
      val v1 = v.updated(v.length - 1, tmp)
      ByteString.ByteStrings(v1, bs.length)
    }
  }

  final case class DropAction(result: ByteString, transformed: Boolean)

  def dropIntelligently(bs: ByteString, n: Int): DropAction = {
    bs match {
      case x: ByteString.ByteStrings => {
        val last = x.bytestrings.last
        if (last.length == x.length - n) {
          DropAction(last, true)
        } else if (last.length > x.length - n) {
          DropAction(last.drop(n - (x.length - last.length)), true)
        } else {
          DropAction(x.drop(n), false)
        }
      }
      case x: ByteString.ByteString1 => DropAction(x.drop(n), false)
      case x: ByteString.ByteString1C => DropAction(x.drop(n), false)
    }
  }

}
