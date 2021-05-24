package io.github.takezoe.trino

import org.apache.commons.codec.digest.DigestUtils

package object checker {
  type QueryResult = Either[Throwable, Seq[Map[String, AnyRef]]]

  def isSame(result1: QueryResult, result2: QueryResult): Boolean = {
    (result1, result2) match {
      case (Left(_), Left(_))     => true
      case (Right(x1), Right(x2)) => checksum(x1) == checksum(x2)
      case _                      => false
    }
  }

  def checksum(rows: Seq[Map[String, AnyRef]]): String = {
    DigestUtils.md5Hex(rows.map { row =>
      row.toSeq
        .sortBy { case (key, _) => key }
        .map    { case (_, value) => value }
        .mkString(",")
    }.sorted.mkString("\n"))
  }
}
