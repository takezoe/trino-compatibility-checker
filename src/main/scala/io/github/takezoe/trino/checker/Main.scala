package io.github.takezoe.trino.checker

import org.apache.commons.codec.digest.DigestUtils

object Main extends App {
  val results = run(Seq(317, 350, 357), "SELECT null GROUP BY 1, 1")

  println("======== result ========")
  results.foreach { case (version, result) =>
    println(s"${version}: ${result.map(checksum)}")
  }

  def run(versions: Seq[Int], sql: String): Seq[(Int, Either[Throwable, Seq[Map[String, AnyRef]]])] = {
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
