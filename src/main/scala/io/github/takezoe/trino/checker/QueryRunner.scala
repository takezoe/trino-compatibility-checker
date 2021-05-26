package io.github.takezoe.trino.checker

import io.prestosql.jdbc.PrestoDriver2
import org.apache.commons.dbutils.{BasicRowProcessor, DbUtils}
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer

import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.util.{Try, Using}
import scala.jdk.CollectionConverters._

trait QueryRunner {
  def runQuery(version: Int, sql: String): QueryResult
}

class DockerQueryRunner extends QueryRunner {
  private val logger = LoggerFactory.getLogger(classOf[QueryRunner])

  override def runQuery(version: Int, sql: String): QueryResult = {
    logger.info(s"Test ${version}")
    Using.resource(createContainer(version)) { container =>
      container.start()
      Try {
        val port = container.getMappedPort(8080)
        waitForStartup(version, port)
        logger.info("Running a test query...")
        runQueryInternal(version, port, sql)
      }.toEither
    }
  }

  private def createContainer(version: Int): GenericContainer[_] = {
    val container = new GenericContainer(if (version >= 351) s"trinodb/trino:${version}" else s"ghcr.io/trinodb/presto:${version}")
    container.addExposedPort(8080)
    container
  }

  private def waitForStartup(version: Int, port: Int): Unit = {
    while (true) {
      try {
        Using.resource(getConnection(version, port)) { conn =>
          runQueryInternal(version, port, "SELECT 1")
          return
        }
      } catch {
        case e: Exception =>
          logger.info(s"Waiting for startup server: ${e.toString}")
          Thread.sleep(5000)
      }
    }
  }

  private def getConnection(version: Int, port: Int): Connection = {
    if (version >= 351) {
      DriverManager.getConnection(s"jdbc:trino://localhost:${port}", "user", "")
    } else {
      new PrestoDriver2().connect(s"jdbc:presto://localhost:${port}?user=user", new Properties())
    }
  }

  private def runQueryInternal(version: Int, port: Int, sql: String): Seq[Map[String, AnyRef]] = {
    Using.resource(getConnection(version, port)) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        Using.resource(stmt.executeQuery(sql)) { rs =>
          val rows = Seq.newBuilder[Map[String, AnyRef]]
          val rowProcessor = new BasicRowProcessor()
          if (rs.next()) {
            rows += rowProcessor.toMap(rs).asScala.toMap
          }
          rows.result()
        }
      }
    }
  }
}
