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

import org.parboiled.scala._
import BasicRules._

private[parser] trait AcceptHeader {
  this: Parser with ProtocolParameterRules with CommonActions ⇒

  def `*Accept` = rule(
    zeroOrMore(MediaRangeDecl, separator = ListSep) ~ EOI ~~> (HttpHeaders.Accept(_)))

  def MediaRangeDecl = rule {
    MediaRangeDef ~ zeroOrMore(";" ~ Parameter) ~~> { (main, sub, params) ⇒
      if (sub == "*") {
        val mainLower = main.toLowerCase
        MediaRanges.getForKey(mainLower) match {
          case Some(registered) ⇒ if (params.isEmpty) registered else registered.withParameters(params.toMap)
          case None             ⇒ MediaRange.custom(mainLower, params.toMap)
        }
      } else {
        val (p, q) = MediaRange.splitOffQValue(params.toMap)
        MediaRange(getMediaType(main, sub, p), q)
      }
    }
  }

  def MediaRangeDef = rule {
    "*/*" ~ push("*", "*") | Type ~ "/" ~ ("*" ~ !TokenChar ~ push("*") | Subtype) | "*" ~ push("*", "*")
  }
}