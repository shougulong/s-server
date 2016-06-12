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

package spray.http

import akka.util.ByteString

/**
 * Models the entity (aka "body" or "content) of an HTTP message.
 */
sealed trait HttpEntity {
  def isEmpty: Boolean
  def nonEmpty: Boolean = !isEmpty
  def data: HttpData
  def flatMap(f: HttpEntity.NonEmpty ⇒ HttpEntity): HttpEntity
  def orElse(other: HttpEntity): HttpEntity
  def asString: String
  def asString(defaultCharset: HttpCharset): String
  def toOption: Option[HttpEntity.NonEmpty]
}

object HttpEntity {
  implicit def apply(string: String): HttpEntity = apply(ContentTypes.`text/plain(UTF-8)`, string)
  implicit def apply(bytes: Array[Byte]): HttpEntity = apply(HttpData(bytes))
  implicit def apply(data: HttpData): HttpEntity = apply(ContentTypes.`application/octet-stream`, data)
  def apply(contentType: ContentType, string: String): HttpEntity =
    if (string.isEmpty) Empty else apply(contentType, HttpData(string, contentType.charset))
  def apply(contentType: ContentType, bytes: Array[Byte]): HttpEntity = apply(contentType, HttpData(bytes))
  def apply(contentType: ContentType, bytes: ByteString): HttpEntity = apply(contentType, HttpData(bytes))
  def apply(contentType: ContentType, data: HttpData): HttpEntity =
    data match {
      case x: HttpData.NonEmpty ⇒ new NonEmpty(contentType, x)
      case _                    ⇒ Empty
    }

  implicit def flatten(optionalEntity: Option[HttpEntity]): HttpEntity =
    optionalEntity match {
      case Some(body) ⇒ body
      case None       ⇒ Empty
    }

  /**
   * Models an empty entity.
   */
  case object Empty extends HttpEntity {
    def isEmpty = true
    def data = HttpData.Empty
    def flatMap(f: HttpEntity.NonEmpty ⇒ HttpEntity): HttpEntity = this
    def orElse(other: HttpEntity): HttpEntity = other
    def asString = ""
    def asString(defaultCharset: HttpCharset) = ""
    def toOption = None
  }

  /**
   * Models a non-empty entity. The buffer array is guaranteed to have a size greater than zero.
   * CAUTION: Even though the byte array is directly exposed for performance reasons all instances of this class are
   * assumed to be immutable! spray never modifies the buffer contents after an HttpEntity.NonEmpty instance has been created.
   * If you modify the buffer contents by writing to the array things WILL BREAK!
   */
  case class NonEmpty private[HttpEntity] (contentType: ContentType, data: HttpData.NonEmpty) extends HttpEntity {
    def isEmpty = false
    def flatMap(f: HttpEntity.NonEmpty ⇒ HttpEntity): HttpEntity = f(this)
    def orElse(other: HttpEntity): HttpEntity = this
    def asString = data.asString(contentType.charset)
    def asString(defaultCharset: HttpCharset) = data.asString(contentType.definedCharset getOrElse defaultCharset)
    def toOption = Some(this)
    override def toString =
      "HttpEntity(" + contentType + ',' + (if (data.length > 500) asString.take(500) + "..." else asString) + ')'
  }
}
