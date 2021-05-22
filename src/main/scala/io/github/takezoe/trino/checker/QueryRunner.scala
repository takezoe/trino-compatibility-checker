package io.github.takezoe.trino.checker

import io.prestosql.jdbc.PrestoDriver2
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer

import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.util.{Try, Using}

class QueryRunner(version: Int) {
  private val logger = LoggerFactory.getLogger(classOf[QueryRunner])

  def runQuery(sql: String): Either[Throwable, Unit] = {
    logger.info(s"Test ${version}")
    Using.resource(createContainer()) { container =>
      container.start()
      Try {
        val port = container.getMappedPort(8080)
        waitForStartup(port)
        logger.info("Running a test query...")
        runQueryInternal(port, sql)
      }.toEither
    }
  }

  private def createContainer(): GenericContainer[_] = {
    val container = new GenericContainer(if (version >= 351) s"trinodb/trino:${version}" else s"ghcr.io/trinodb/presto:${version}")
    container.addExposedPort(8080)
    container
  }

  private def waitForStartup(port: Int): Unit = {
    while (true) {
      try {
        Using.resource(getConnection(port)) { conn =>
          runQueryInternal(port, "SELECT 1")
          return
        }
      } catch {
        case e: Exception =>
          logger.info(s"Waiting for startup server: ${e.toString}")
          Thread.sleep(5000)
      }
    }
  }

  private def getConnection(port: Int): Connection = {
    if (version >= 351) {
      DriverManager.getConnection(s"jdbc:trino://localhost:${port}", "user", "")
    } else {
      new PrestoDriver2().connect(s"jdbc:presto://localhost:${port}?user=user", new Properties())
    }
  }

  private def runQueryInternal(port: Int, sql: String) = {
    Using.resource(getConnection(port)) { conn =>
      Using.resource(conn.createStatement()) { stmt =>
        Using.resource(stmt.executeQuery(sql)) { rs =>
          if (rs.next()) {
            // TODO
          }
        }
      }
    }
  }
}
