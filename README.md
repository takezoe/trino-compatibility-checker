trino-compatibility-checker
========

Automated test tool for comparing behavior between versions of Trino/Presto.

This tool can be used to find a Trino/Presto version that introduced a different behavior by comparing the query results between versions using docker.

```scala
import io.github.takezoe.trino.checker._

val checker = new BinaryVersionChecker(new DockerQueryRunner())
val results = checker.check(Range.inclusive(317, 350), "SELECT null GROUP BY 1, 1")

results.headOption.foreach { case (_, firstResult) =>
  results.foreach { case (version, result) =>
    println(s"${if(isSame(firstResult, result)) "✅" else "❌"} ${version}: ${result.map(checksum)}")
  }
}
```

The execution result of code above is below. In this case, 339 is a version that introduced a different behavior.

```
✅ 317: Right(37a6259cc0c1dae299a7866489dff0bd)
❌ 350: Left(java.sql.SQLException: Query failed (#20210526_154140_00004_yzz4q): Multiple entries with same key: @38f546e: null=expr and @38f546e: null=expr)
✅ 334: Right(37a6259cc0c1dae299a7866489dff0bd)
❌ 342: Left(java.sql.SQLException: Query failed (#20210526_154251_00003_2km75): Multiple entries with same key: @63f11e0f: null=expr and @63f11e0f: null=expr)
✅ 338: Right(37a6259cc0c1dae299a7866489dff0bd)
❌ 340: Left(java.sql.SQLException: Query failed (#20210526_154338_00002_2xtvz): Multiple entries with same key: @76615946: null=expr and @76615946: null=expr)
❌ 339: Left(java.sql.SQLException: Query failed (#20210526_154358_00002_jdnni): Multiple entries with same key: @4aca599d: null=expr and @4aca599d: null=expr)
```
