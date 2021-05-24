package io.github.takezoe.trino.checker

import org.apache.commons.codec.digest.DigestUtils

object Main extends App {
  val results = binarySearch(Range.inclusive(317, 350), "SELECT null GROUP BY 1, 1")

  println("======== result ========")
  results.headOption.foreach { case (_, firstResult) =>
    results.foreach { case (version, result) =>
      println(s"${if(isSame(firstResult, result)) "✅" else "❌"} ${version}: ${result.map(checksum)}")
    }
  }

  def checkAll(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    versions.map { version =>
      (version, new QueryRunner(version).runQuery(sql))
    }
  }

  def linerSearch(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    val results = Seq.newBuilder[(Int, QueryResult)]
    val firstVersion = versions.head
    val firstResult = (firstVersion, new QueryRunner(firstVersion).runQuery(sql))
    results += firstResult

    versions.tail.takeWhile { version =>
      val result = (version, new QueryRunner(version).runQuery(sql))
      results += result
      isSame(firstResult._2, result._2)
    }

    results.result()
  }

  def binarySearch(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    val results = Seq.newBuilder[(Int, QueryResult)]
    val firstVersion = versions.head
    val firstResult = (firstVersion, new QueryRunner(firstVersion).runQuery(sql))
    results += firstResult

    def searchInternal(left: Seq[Int], right: Seq[Int], version: Int): Unit = {
      val result = (version, new QueryRunner(version).runQuery(sql))
      results += result

      if (isSame(firstResult._2, result._2)) {
        if (right.nonEmpty) {
          val index = right.size / 2
          val nextVersion = right(index)
          val nextLeft = right.slice(0, index)
          val nextRight = right.slice(index + 1, right.size)
          searchInternal(nextLeft, nextRight, nextVersion)
        }
      } else {
        if (left.nonEmpty) {
          val index = left.size / 2
          val nextVersion = left(index)
          val nextLeft = left.slice(0, index)
          val nextRight = left.slice(index + 1, left.size)
          searchInternal(nextLeft, nextRight, nextVersion)
        }
      }
    }

    searchInternal(versions.slice(1, versions.size - 1), Seq.empty, versions.last)
    results.result()
  }

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
