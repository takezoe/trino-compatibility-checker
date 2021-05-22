package io.github.takezoe.trino.checker

object Main extends App {
  val results = run(Seq(317, 350, 357), "SELECT null GROUP BY 1, 1")

  println("======== result ========")
  results.foreach { case (version, result) =>
    println(s"${version}: ${result}")
  }

  def run(versions: Seq[Int], sql: String): Seq[(Int, Either[Throwable, Unit])] = {
    versions.map { version =>
      val runner = new QueryRunner(version)
      (version, runner.runQuery(sql))
    }
  }
}
