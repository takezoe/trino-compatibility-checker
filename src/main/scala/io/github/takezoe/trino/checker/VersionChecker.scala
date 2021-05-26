package io.github.takezoe.trino.checker

trait VersionChecker {
  def check(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)]
}

class AllVersionChecker(runner: QueryRunner) extends VersionChecker {
  override def check(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    versions.map { version =>
      (version, runner.runQuery(version, sql))
    }
  }
}

class LinerVersionChecker(runner: QueryRunner) extends VersionChecker {
  override def check(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    val results = Seq.newBuilder[(Int, QueryResult)]
    val firstVersion = versions.head
    val firstResult = (firstVersion, runner.runQuery(firstVersion, sql))
    results += firstResult

    versions.tail.takeWhile { version =>
      val result = (version, runner.runQuery(version, sql))
      results += result
      isSame(firstResult._2, result._2)
    }

    results.result()
  }
}

class BinaryVersionChecker(runner: QueryRunner) extends VersionChecker {
  override def check(versions: Seq[Int], sql: String): Seq[(Int, QueryResult)] = {
    val results = Seq.newBuilder[(Int, QueryResult)]
    val firstVersion = versions.head
    val firstResult = (firstVersion, runner.runQuery(firstVersion, sql))
    results += firstResult

    def searchInternal(left: Seq[Int], right: Seq[Int], version: Int): Unit = {
      val result = (version, runner.runQuery(version, sql))
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
}