package io.github.takezoe.trino.checker

import org.apache.commons.codec.digest.DigestUtils

object Main extends App {
  val results = checkAll(Seq(317, 350, 357), "SELECT null GROUP BY 1, 1")

  println("======== result ========")
  results.headOption.foreach { case (_, firstResult) =>
    results.foreach { case (version, result) =>
      println(s"${if(isSame(firstResult, result)) "✅" else "❌"} ${version}: ${result.map(checksum)}")
    }
  }

  def checkAll(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    versions.map { version =>
      val runner = new QueryRunner(version)
      (version, runner.runQuery(sql))
    }
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
