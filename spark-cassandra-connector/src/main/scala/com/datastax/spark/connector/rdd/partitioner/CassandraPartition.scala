package com.datastax.spark.connector.rdd.partitioner

import java.net.InetAddress

import org.apache.spark.Partition

import com.datastax.spark.connector.rdd.partitioner.dht.{Token, TokenFactory, TokenRange}

/** Stores a CQL `WHERE` predicate matching a range of tokens. */
case class CqlTokenRange[V, T <: Token[V]](range: TokenRange[V, T])(implicit tf: TokenFactory[V, T]) {

  require(!range.isWrappedAround)

  // YugaByte token ranges are start-inclusive and end-exclusive. So we always use ">=" for lower
  // bounds and "<" for upper bounds to fit the ranges exactly.
  def cql(pk: String): (String, Seq[Any]) =
    if (range.start == tf.minToken && range.end == tf.minToken) // entire ring => 'tk >= minToken'
      (s"token($pk) >= ?", Seq(range.start.value))
    else if (range.start == tf.minToken) // start is minToken => explicit lower bound not needed.
      (s"token($pk) < ?", Seq(range.end.value))
    else if (range.end == tf.minToken) // end is minToken => explicit upper bound not needed.
      (s"token($pk) >= ?", Seq(range.start.value))
    else
      (s"token($pk) >= ? AND token($pk) < ?", Seq(range.start.value, range.end.value))
}

trait EndpointPartition extends Partition {
  def endpoints: Iterable[InetAddress]
}

/** Metadata describing Cassandra table partition processed by a single Spark task.
  * Beware the term "partition" is overloaded. Here, in the context of Spark,
  * it means an arbitrary collection of rows that can be processed locally on a single Cassandra cluster node.
  * A `CassandraPartition` typically contains multiple CQL partitions, i.e. rows identified by different values of
  * the CQL partitioning key.
  *
  * @param index identifier of the partition, used internally by Spark
  * @param endpoints which nodes the data partition is located on
  * @param tokenRanges token ranges determining the row set to be fetched
  * @param dataSize estimated amount of data in the partition
  */
case class CassandraPartition[V, T <: Token[V]](
  index: Int,
  endpoints: Iterable[InetAddress],
  tokenRanges: Iterable[CqlTokenRange[V, T]],
  dataSize: Long) extends EndpointPartition

