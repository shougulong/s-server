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
package parser

import spray.http.MediaTypes._
import org.parboiled.errors.ParsingException

private[parser] trait CommonActions {

  type StringMapBuilder = scala.collection.mutable.Builder[(String, String), Map[String, String]]

  def getMediaType(mainType: String, subType: String, parameters: Map[String, String]): MediaType = {
    mainType.toLowerCase match {
      case "multipart" ⇒ subType.toLowerCase match {
        case "mixed"       ⇒ multipart.mixed(parameters)
        case "alternative" ⇒ multipart.alternative(parameters)
        case "related"     ⇒ multipart.related(parameters)
        case "form-data"   ⇒ multipart.`form-data`(parameters)
        case "signed"      ⇒ multipart.signed(parameters)
        case "encrypted"   ⇒ multipart.encrypted(parameters)
        case custom        ⇒ multipart(custom, parameters)
      }
      case mainLower ⇒
        MediaTypes.getForKey((mainLower, subType.toLowerCase)) match {
          case Some(registered) ⇒ if (parameters.isEmpty) registered else registered.withParameters(parameters)
          case None             ⇒ MediaType.custom(mainType, subType, parameters = parameters, allowArbitrarySubtypes = true)
        }
    }
  }

  def getCharset(name: String): HttpCharset =
    HttpCharsets
      .getForKey(name.toLowerCase)
      .orElse(HttpCharset.custom(name))
      .getOrElse(throw new ParsingException("Unsupported charset: " + name))
}