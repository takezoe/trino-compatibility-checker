package io.github.takezoe.trino.checker

object Main extends App {
  val checker = new BinaryVersionChecker(new DockerQueryRunner())
  val results = checker.check(Range.inclusive(317, 350), "SELECT null GROUP BY 1, 1")
  printResults(results)
}
