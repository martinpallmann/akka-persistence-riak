package akka.persistence.riak

import akka.persistence.{ PersistentConfirmation, PersistentRepr }
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
import scala.util.Try

case class Riak(addresses: List[String], minConnections: Int, maxConnections: Int) {
  private lazy val cluster = {
    val nodes = RiakNode.Builder()
      .withMinConnections(minConnections)
      .withMaxConnections(maxConnections)
      .buildNodes(addresses)
    RiakCluster.Builder(nodes).build
  }

  private lazy val client = {
    cluster.start()
    RiakClient(cluster)
  }

  def shutdown() = cluster.shutdown()

  def delete(pId: PersistId, sNr: SeqNr, permanent: Boolean)(implicit ec: ExecutionContext, bucketType: JournalBucketType): Future[Unit] = permanent match {

    case true  => client deleteValue Location(pId, sNr)
    case false => client updateMap (Location(pId, sNr) -> Seq(MapActions.UpdateFlag("deleted", flag = true)))
  }

  def storeMessages(messages: Iterable[PersistentRepr])(implicit ec: ExecutionContext, ser: Serialization, bucketType: JournalBucketType): Future[Iterable[(PersistId, SeqNr)]] = Future.sequence {

    def toUpdate(msgs: Iterable[PersistentRepr]) = msgs.map(msg => MapActions.UpdateRegister("payload", serialize(msg)))

    messages.groupBy(msg => (PersistId(msg.persistenceId), SeqNr(msg.sequenceNr))).map {
      case ((pId, seqNr), msgs) => client updateMap (Location(pId, seqNr) -> toUpdate(msgs)) map (_ => pId -> seqNr)
    }
  }

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

  def fetchMessage(pId: PersistId, seqNr: SeqNr)(implicit ec: ExecutionContext, ser: Serialization, bucketType: JournalBucketType): Future[Option[PersistentRepr]] =
    client fetchMap Location(pId, seqNr) map (resp => deserialize(resp))

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

  @deprecated("writeConfirmations will be removed, since Channels will be removed.", since = "0.1")
  def writeConfirmations(confirmations: Seq[PersistentConfirmation])(implicit ec: ExecutionContext, bucketType: JournalBucketType): Future[Unit] = Future.sequence {
    confirmations
      .groupBy(c => Location(PersistId(c.persistenceId), SeqNr(c.sequenceNr)))
      .map {
        case (loc, cs) =>
          client updateMap loc -> Seq(
            MapActions.UpdateSet("confirmations", cs.map(c => SetActions.Add(BinaryValue createFromUtf8 c.channelId)))
          )
      }
  }.map(_ => ())

  private def serialize(p: PersistentRepr)(implicit ser: Serialization): BinaryValue =
    BinaryValue create ser.serializerFor(classOf[PersistentRepr]).toBinary(p)

  private def deserialize(response: FetchMap.Response)(implicit serialization: Serialization): Option[PersistentRepr] = {

    val data = for {
      riakMap <- Option(response.getDatatype)
    } yield riakMap

    val payload = for {
      d <- data
      reg <- Try(d.getRegister(BinaryValue createFromUtf8 "payload")).toOption
      value <- Option(reg.getValue)
      result <- Option(value.getValue)
    } yield serialization.serializerFor(classOf[PersistentRepr]).fromBinary(result).asInstanceOf[PersistentRepr]

    val del = for {
      d <- data
      value <- Try(d.getFlag(BinaryValue createFromUtf8 "deleted")).toOption
    } yield value.getEnabled

    val confirmations = for {
      d <- data.toList
      value <- Try(d.getSet("confirmations")).toOption.toList
      res <- value.view().asScala.map(_.toStringUtf8).toList
    } yield res

    payload.map(_.update(deleted = del.getOrElse(false)).update(confirms = confirmations))
  }
}
