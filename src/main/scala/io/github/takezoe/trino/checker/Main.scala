package io.github.takezoe.trino.checker

object Main extends App {
  val checker = new BinaryVersionChecker(new DockerQueryRunner())
  val results = checker.check(Range.inclusive(317, 350), "SELECT null GROUP BY 1, 1")

  println("======== result ========")
  results.headOption.foreach { case (_, firstResult) =>
    results.foreach { case (version, result) =>
      println(s"${if(isSame(firstResult, result)) "✅" else "❌"} ${version}: ${result.map(checksum)}")
    }
  }
}
