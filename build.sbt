name := "trino-compatibility-checker"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.testcontainers" % "testcontainers" % "1.15.3",
  "io.trino" % "trino-jdbc" % "357",
  "io.prestosql" % "presto-jdbc" % "350"
)