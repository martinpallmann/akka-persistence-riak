package akka.persistence.journal.riak

import java.nio.charset.Charset

import akka.actor.ActorLogging
import akka.persistence.PersistentRepr
import akka.persistence.journal.{ AsyncWriteJournal, SyncWriteJournal }
import akka.serialization.{ SerializationExtension, Serialization }
import com.basho.riak.client.api.annotations.RiakUsermeta
import com.basho.riak.client.api.cap.Quorum
import com.basho.riak.client.api.commands.datatypes.UpdateSet.Builder
import com.basho.riak.client.api.commands.datatypes._
import com.basho.riak.client.api.commands.kv.{ FetchValue, StoreValue }
import com.basho.riak.client.api.commands.kv.StoreValue.Response
import com.basho.riak.client.api.{ RiakCommand, RiakClient }
import com.basho.riak.client.core.query.{ RiakObject, Location, Namespace }
import com.basho.riak.client.core.util.BinaryValue
import com.basho.riak.client.core.{ RiakCluster, RiakNode }
import com.typesafe.config.Config
import scala.annotation.tailrec
import scala.collection.immutable.NumericRange.Inclusive
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

class RiakAsyncWriteJournalOld /* extends AsyncWriteJournal
    with DeprecatedRiakAsyncWriteJournal with ActorLogging */ {
  /*
  private[this] val journalBucketType = "journal2" // TODO from config

  private[this] val journalSeqNrsBucketType = "journal-seqnrs" // TODO from config

  private[this] val serialization: Serialization = SerializationExtension(context.system)

  private[this] val writeQuorum = new Quorum(0)

  private[this] val bucketType = BinaryValue createFromUtf8 journalBucketType

  private[this] def bucketName(key: String) = BinaryValue createFromUtf8 key

  private[this] def nameSpace(key: String) = new Namespace(bucketType, bucketName(key))

  private[this] val writeTimeout = (60 seconds).toMillis.asInstanceOf[Int]

  private[this] def location(persistenceId: String, sequenceNr: Long) =
    new Location(nameSpace(persistenceId), sequenceNr.toString)

  private[this] def seqNrLocation(persistenceId: String) =
    new Location(new Namespace(BinaryValue createFromUtf8 journalSeqNrsBucketType, bucketName("sequenceNrs")), persistenceId)

  private[this] def counterLocation(persistenceId: String) = new Location(nameSpace(s"sequenceNr"), persistenceId)

  private[this] lazy val riak = {
    val builder = new RiakNode.Builder()
    builder.withMinConnections(10)
    builder.withMaxConnections(50)
    val addresses = "127.0.0.1" :: Nil
    val nodes = RiakNode.Builder.buildNodes(builder, addresses.asJava)
    val cluster = new RiakCluster.Builder(nodes).build()
    cluster.start()
    new RiakClient(cluster)
  }

  private[this] def sequenceNrs(persistenceId: String): Future[mutable.Set[Long]] = {
    val fetch = new FetchSet.Builder(seqNrLocation(persistenceId)).build()
    (riak executeAsync fetch).asScalaFuture map (_.getDatatype.view().asScala map (_.toString.toLong))
  }

  private[this] def put(persistenceId: String, sequenceNr: Long, value: Array[Byte]): Future[Unit] = {

    val riakObject = new RiakObject()
    riakObject setValue (BinaryValue create value)
    val store = new StoreValue.Builder(riakObject)
      .withLocation(location(persistenceId, sequenceNr))
      .withOption(StoreValue.Option.W, writeQuorum)
      .withTimeout(writeTimeout)
      .build()
    val f0 = (riak executeAsync store).asScalaFuture
    val set = new UpdateSet.Builder(seqNrLocation(persistenceId), new SetUpdate()
      .add(BinaryValue.createFromUtf8(s"$sequenceNr")))
      .build()
    val f1 = (riak executeAsync set).asScalaFuture
    for {
      _ <- f0
      _ <- f1
    } yield ()
  }

  private[this] def get(persistenceId: String, sequenceNr: Long): Future[Option[Array[Byte]]] = {
    val fetch = new FetchValue.Builder(location(persistenceId, sequenceNr)).build()
    (riak executeAsync fetch).asScalaFuture map { v =>
      {
        if (v.isNotFound) None
        else Some(v.getValue(classOf[RiakObject]).getValue.getValue)
      }
    }
  }

  override def asyncWriteMessages(messages: Seq[PersistentRepr]): Future[Unit] = {
    println(s"write ${messages.size} persistent messages")
    Future.sequence(messages map {
      case m => for {
        value <- serialize(m).asFuture
        _ <- put(m.persistenceId, m.sequenceNr, value)
      } yield ()
    }).map(_ => { () })
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long, permanent: Boolean): Future[Unit] = {
    println(s"delete messages for persistenceId: $persistenceId to sequenceNr: $toSequenceNr, permanent: $permanent")
    Future.successful(())
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: (PersistentRepr) => Unit): Future[Unit] = {
    println(s"replay messages for persistenceId: $persistenceId from sequenceNr: $fromSequenceNr to sequenceNr: $toSequenceNr with max records: $max")
    def cb(msg: PersistentRepr): Unit = {
      println("  " + msg.persistenceId + " " + msg.sequenceNr)
      replayCallback(msg)
    }
    def replayM(fromSequenceNr: Long, max: Long): Future[Unit] = {
      if (fromSequenceNr > toSequenceNr || max <= 0L) Future.successful(())
      else {
        get(persistenceId, fromSequenceNr) map (_.flatMap(deserialize)) map {
          case Some(v) =>
            cb(v)
            replayM(fromSequenceNr + 1, max - 1)
          case _ =>
            Future.failed(new RuntimeException("not found"))
        }
      }
    }
    replayM(fromSequenceNr, max)
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    println(s"read highest sequenceNr for persistenceId: $persistenceId from sequenceNr $fromSequenceNr")
    sequenceNrs(persistenceId).map(_.max)
  }

  private def serialize(o: PersistentRepr) = serialization.serialize(o)

  private def deserialize(b: Array[Byte]) = serialization.deserialize(b, classOf[PersistentRepr]).toOption

  private def asF[A](tr: Try[A]) = tr match {
    case Success(v) => Future.successful(v)
    case Failure(t) => Future.failed(t)
  }
  */
}

