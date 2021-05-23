package io.github.takezoe.trino.checker

import org.apache.commons.codec.digest.DigestUtils

object Main extends App {
  val results = checkAll(Seq(317, 350, 357), "SELECT null GROUP BY 1, 1")

  val checksums = results.map { case (version, result) =>
    (version, result.map(checksum))
  }
  println("======== result ========")
  checksums.headOption.foreach { case (_, firstResult) =>
    checksums.foreach { case (version, result) =>
      println(s"${if(firstResult == result) "✅" else "❌"} ${version}: ${result}")
    }
  }

  def checkAll(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    versions.map { version =>
      val runner = new QueryRunner(version)
      (version, runner.runQuery(sql))
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
