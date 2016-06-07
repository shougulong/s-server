package woshilaiceshide.sserver.http

import scala.annotation.tailrec
import spray.http.HttpHeaders._

object OptimizedUtility {

  def isUpgrade(s: String): Boolean = {
    val len = 7 //"upgrade".length
    s.length() == len &&
      ('U' == s.charAt(0) || 'u' == s.charAt(0)) &&
      ('p' == s.charAt(1) || 'P' == s.charAt(1)) &&
      ('g' == s.charAt(2) || 'G' == s.charAt(2)) &&
      ('r' == s.charAt(3) || 'R' == s.charAt(3)) &&
      ('a' == s.charAt(4) || 'A' == s.charAt(4)) &&
      ('d' == s.charAt(5) || 'D' == s.charAt(5)) &&
      ('e' == s.charAt(6) || 'E' == s.charAt(6))
  }

  def hasUpgrade(c: Connection): Boolean = {

    var these = c.tokens
    while (!these.isEmpty) {
      if (isUpgrade(these.head)) return true
      these = these.tail
    }
    return false

  }

  def isClose(s: String): Boolean = {
    val len = 5 //"close".length
    s.length() == len &&
      ('C' == s.charAt(0) || 'c' == s.charAt(0)) &&
      ('l' == s.charAt(1) || 'L' == s.charAt(1)) &&
      ('o' == s.charAt(2) || 'O' == s.charAt(2)) &&
      ('s' == s.charAt(3) || 'S' == s.charAt(3)) &&
      ('e' == s.charAt(4) || 'E' == s.charAt(4))
  }

  def hasClose(c: Connection): Boolean = {

    var these = c.tokens
    while (!these.isEmpty) {
      if (isClose(these.head)) return true
      these = these.tail
    }
    return false

  }

  def isNotKeepAlive(s: String): Boolean = {
    val len = 10 //"keep-alive".length
    s.length() != len ||
      ('K' != s.charAt(0) && 'k' != s.charAt(0)) ||
      ('e' != s.charAt(1) && 'E' != s.charAt(1)) ||
      ('e' != s.charAt(2) && 'E' != s.charAt(2)) ||
      ('p' != s.charAt(3) && 'P' != s.charAt(3)) ||
      ('-' != s.charAt(4)) ||
      ('A' != s.charAt(5) && 'a' != s.charAt(5)) ||
      ('l' != s.charAt(6) && 'L' != s.charAt(6)) ||
      ('i' != s.charAt(7) && 'I' != s.charAt(7)) ||
      ('v' != s.charAt(8) && 'V' != s.charAt(8)) ||
      ('e' != s.charAt(9) && 'E' != s.charAt(9))
  }

  def isKeepAlive(s: String): Boolean = {
    !isNotKeepAlive(s)
  }

  def hasNoKeepAlive(c: Connection): Boolean = {

    var these = c.tokens
    while (!these.isEmpty) {
      if (isKeepAlive(these.head)) return false
      these = these.tail
    }
    return true
  }

}