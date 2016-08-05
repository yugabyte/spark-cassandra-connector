package com.datastax.spark.connector.rdd.reader

import com.datastax.spark.connector.CassandraRowMetadata
import com.datastax.spark.connector.cql._
import com.datastax.spark.connector.embedded.SparkTemplate
import com.datastax.spark.connector.rdd.reader.ClassBasedRowReaderBenchmark.{ KeyspaceName, RowsCount, TableName }
import org.apache.spark.sql.cassandra.CassandraSQLRow
import org.openjdk.jmh.annotations._

import scala.collection.JavaConversions._

@State(Scope.Thread)
class ClassBasedRowReaderBenchmark extends SparkTemplate {

  case class KeyValue(key: Int, value: String)

  val config = defaultConf.set("spark.cassandra.connection.port", "9042")

  val connector = CassandraConnector(config)

  val table = Schema.tableFromCassandra(connector, KeyspaceName, TableName)

  val resultSet = connector.withSessionDo { session =>
    session.execute(s"SELECT * FROM $KeyspaceName.$TableName")
  }

  val rows = resultSet.all().toIndexedSeq

  val rowMetaData = CassandraRowMetadata.fromResultSet(table.columns.map(_.columnName), resultSet)

  val oldReader = new ClassBasedRowReaderOld[KeyValue](table, table.columnRefs)

  val newReader = new ClassBasedRowReader[KeyValue](table, table.columnRefs)

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OperationsPerInvocation(RowsCount)
  def oldClassRowReader(): Seq[KeyValue] = {
    rows.map { row =>
      oldReader.read(row, rowMetaData)
    }
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OperationsPerInvocation(RowsCount)
  def newClassRowReader(): Seq[KeyValue] = {
    rows.map { row =>
      newReader.read(row, rowMetaData)
    }
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OperationsPerInvocation(RowsCount)
  def dfReader(): Seq[CassandraSQLRow] = {
    rows.map { row =>
      CassandraSQLRow.fromJavaDriverRow(row, rowMetaData)
    }
  }

}

object ClassBasedRowReaderBenchmark {
  val KeyspaceName = "ClassBasedRowReaderBenchmark".toLowerCase
  val TableName = "keyValue".toLowerCase
  final val RowsCount = 100000
}