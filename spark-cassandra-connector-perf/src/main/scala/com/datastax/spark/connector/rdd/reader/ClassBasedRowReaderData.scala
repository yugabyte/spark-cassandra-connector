package com.datastax.spark.connector.rdd.reader

import com.datastax.driver.core.Session
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.embedded.SparkTemplate
import com.datastax.spark.connector.rdd.reader.ClassBasedRowReaderBenchmark.{ KeyspaceName, RowsCount, TableName }

object ClassBasedRowReaderData extends App with SparkTemplate {

  val config = defaultConf.set("spark.cassandra.connection.port", "9042")

  val someContent =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam quis augue
      tristique, ultricies nisl sit amet, condimentum felis. Ut blandit nisi eget imperdiet pretium. Ut a
      erat in tortor ultrices posuere quis eu dui. Lorem ipsum dolor sit amet, consectetur adipiscing
      elit. Integer vestibulum vitae arcu ac vehicula. Praesent non erat quis ipsum tempor tempus
      vitae quis neque. Aenean eget urna egestas, lobortis velit sed, vestibulum justo. Nam nibh
      risus, bibendum non ex ac, bibendum varius purus. """

  def createKeyspaceCql(name: String) =
    s"""
       |CREATE KEYSPACE IF NOT EXISTS $name
       |WITH REPLICATION = { 'class': 'SimpleStrategy', 'replication_factor': 1 }
       |AND durable_writes = false
       |""".stripMargin

  def createTableCql(keyspace: String, tableName: String): String =
    s"""
       |CREATE TABLE $keyspace.$tableName (
       |  key INT,
       |  value TEXT,
       |  PRIMARY KEY (key)
       |)""".stripMargin

  def insertData(session: Session, keyspace: String, table: String, rows: Int): Unit = {
    for (i <- 0 until rows)
      session.execute(
        s"INSERT INTO $KeyspaceName.$table (key, value) VALUES (?, ?)", i: Integer, s"${i}_${someContent}".toString
      )
  }

  val conn = CassandraConnector(config)
  conn.withSessionDo { session =>
    session.execute(s"DROP KEYSPACE IF EXISTS $KeyspaceName")
    session.execute(createKeyspaceCql(KeyspaceName))
    session.execute(createTableCql(KeyspaceName, TableName))

    insertData(session, KeyspaceName, TableName, RowsCount)
  }
}
