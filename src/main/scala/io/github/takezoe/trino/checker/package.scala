package io.github.takezoe.trino

package object checker {
  type QueryResult = Either[Throwable, Seq[Map[String, AnyRef]]]
}
