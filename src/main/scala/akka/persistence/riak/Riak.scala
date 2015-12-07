package akka.persistence.riak

import akka.persistence.PersistentRepr
import akka.persistence.riak.client.api.RiakClient.MapActions.UpdateRegister
import akka.persistence.riak.client.api.RiakClient.{ SetActions, MapActions }
import akka.persistence.riak.client.api.RiakClient.SetActions.Add
import akka.persistence.riak.client.core.{ RiakCluster, RiakNode }
import akka.persistence.riak.client.api.RiakClient
import akka.persistence.riak.Implicits._
import akka.serialization.Serialization
import com.basho.riak.client.api.commands.datatypes.FetchMap
import com.basho.riak.client.api.commands.datatypes.UpdateSet.Response
import com.basho.riak.client.core.util.BinaryValue
import scala.collection.immutable.{ Iterable, Seq }
import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

case class Riak(addresses: List[String], minConnections: Int, maxConnections: Int) {
  private lazy val cluster = {
    val nodes = RiakNode.Builder()
      .withMinConnections(minConnections)
      .withMaxConnections(maxConnections)
      .buildNodes(addresses)
    RiakCluster.Builder(nodes).build
  }

  private lazy val client = Try {
    cluster.start()
    RiakClient(cluster)
  } match {
    case Success(s) => s
    case Failure(f) =>
      f.printStackTrace()
      throw f
  }

  def shutdown() = cluster.shutdown()

  def delete(pId: PersistId, sNr: SeqNr)(implicit ec: ExecutionContext, bucketType: JournalBucketType): Future[Unit] =
    client deleteValue Location(pId, sNr)

  def serializeMsgs(messages: Iterable[PersistentRepr])(implicit ser: Serialization): Try[Iterable[(String, Long, BinaryValue)]] = {
    messages.map(v => (v.persistenceId, v.sequenceNr, serialize(v))).foldLeft(Success(Nil): Try[Seq[(String, Long, BinaryValue)]]) {
      case (Success(acc), (p, s, Success(elem))) => Success(acc :+ (p, s, elem))
      case (_, (_, _, Failure(t)))               => Failure(t)
      case (Failure(t), _)                       => Failure(t)
    }
  }

  def storeMessages(messages: Iterable[(String, Long, BinaryValue)])(implicit ec: ExecutionContext, ser: Serialization, bucketType: JournalBucketType): Future[Iterable[(PersistId, SeqNr)]] = Future.sequence {

    def toUpdate(msgs: Iterable[(String, Long, BinaryValue)]) = msgs.map(msg => MapActions.UpdateRegister("payload", msg._3))

    messages.groupBy(msg => (PersistId(msg._1), SeqNr(msg._2))).map {
      case ((pId, seqNr), msgs) => client updateMap (Location(pId, seqNr) -> toUpdate(msgs)) map (_ => pId -> seqNr)
    }
  }

  def fetchMessage(pId: PersistId, seqNr: SeqNr)(implicit ec: ExecutionContext, ser: Serialization, bucketType: JournalBucketType): Future[Option[PersistentRepr]] =
    client fetchMap Location(pId, seqNr) map (resp => deserialize(resp))

  def storeSeqNrs(seqNrs: Iterable[(PersistId, SeqNr)])(implicit ec: ExecutionContext, bucketType: JournalBucketType): Future[Iterable[Response]] = {

    val key = (entry: (PersistId, SeqNr)) => entry match { case (k, _) => k }

    Future.sequence {
      (seqNrs groupBy key).map {
        case (pId, entries) => Location(pId) -> (entries map {
          case (_, seqNr) => Add(seqNr.toBinaryValue)
        })
      } map client.updateSet
    }
  }

  def fetchSequenceNrs(pId: PersistId,
    filter: SeqNr => Boolean = alwaysTrue,
    max: Option[SeqNr] = None)(
      implicit ec: ExecutionContext,
      bucketType: JournalBucketType): Future[Seq[SeqNr]] =

    for {
      res <- client fetchSet Location(pId)
    } yield {
      val result = res.getDatatype.view()
        .asScala
        .map(SeqNr.apply)
        .filter(filter)
        .toList
        .sortWith(_ < _)
      max match {
        case Some(m) => result.take(m)
        case _       => result
      }
    }

  private def serialize(p: PersistentRepr)(implicit ser: Serialization): Try[BinaryValue] = Try {
    BinaryValue create ser.serializerFor(classOf[PersistentRepr]).toBinary(p)
  }

  private def deserialize(response: FetchMap.Response)(implicit ser: Serialization): Option[PersistentRepr] = {

    def fromBinary(value: Array[Byte])(implicit ser: Serialization): Option[PersistentRepr] = Try {
      ser.serializerFor(classOf[PersistentRepr]).fromBinary(value).asInstanceOf[PersistentRepr]
    }.toOption

    val data = for {
      riakMap <- Option(response.getDatatype)
    } yield riakMap

    for {
      d <- data
      reg <- Try(d.getRegister(BinaryValue createFromUtf8 "payload")).toOption
      value <- Option(reg.getValue.getValue)
      result <- fromBinary(value)
    } yield result
  }
}
