package akka.persistence.journal.riak

import akka.actor.ActorLogging
import akka.serialization.SerializationExtension
import scala.collection.immutable
import scala.concurrent.Future
import scala.collection.immutable.Seq
import akka.persistence.{ AtomicWrite, PersistentRepr }
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.riak._
import akka.persistence.riak.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.collection.JavaConverters._

class RiakAsyncWriteJournal extends AsyncWriteJournal with ActorLogging {

  private val root = "akka-persistence-riak-async"
  private lazy val config = context.system.settings.config
  implicit lazy val serialization = SerializationExtension(context.system)
  implicit lazy val bucketType: JournalBucketType = JournalBucketType(config getString (s"$root.journal.bucket-type", "journal"))

  private lazy val nodes = (config getStringList s"$root.nodes").asScala.toList
  private lazy val riak: Riak = Riak(nodes, 1, 50)

  context.system.registerOnTermination(riak shutdown ())

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]): Future[immutable.Seq[Try[Unit]]] =
    Future.sequence {
      messages.map(asyncWriteMessage)
    }

  def asyncWriteMessage(message: AtomicWrite): Future[Try[Unit]] = {
    val msgs = riak serializeMsgs message.payload
    msgs match {
      case Failure(t) => Future.successful(Failure(t))
      case Success(res) => for {
        seqNrs <- riak storeMessages res
        res <- riak storeSeqNrs seqNrs
      } yield Success(Unit)
    }
  }

  /**
   * asynchronously deletes all persistent messages up to `toSequenceNr` (inclusive)
   * from the riak journal.
   */
  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    val pId = PersistId(persistenceId)
    for {
      sNrs <- riak fetchSequenceNrs (pId, _ <= SeqNr(toSequenceNr))
      _ <- delete(pId, sNrs)
    } yield ()
  }

  private def delete(pId: PersistId, sNrs: Seq[SeqNr]): Future[Seq[Unit]] = Future.sequence {
    sNrs map (sNr => riak delete (pId, sNr))
  }

  /**
   * asynchronously reads the highest stored sequence number for the
   * given `persistenceId` from the riak journal
   */
  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = for {
    seqNrs <- riak fetchSequenceNrs PersistId(persistenceId)
  } yield if (seqNrs.isEmpty) 0L else seqNrs.maxBy(_.value).value

  /**
   * asynchronously replays persistent messages. Implementations replay
   * a message by calling `replayCallback`. The returned future must be completed
   * when all messages (matching the sequence number bounds) have been replayed.
   * The future must be completed with a failure if any of the persistent messages
   * could not be replayed.
   */
  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: (PersistentRepr) => Unit): Future[Unit] = {

    def isInRange(seqNr: SeqNr) = seqNr.value >= fromSequenceNr && seqNr.value <= toSequenceNr

    def replayCallbacks(seqNrs: Seq[SeqNr]): Future[Unit] = seqNrs.foldLeft(Future.successful(())) {
      (acc, elem) =>
        for {
          _ <- acc
          msg <- riak fetchMessage (PersistId(persistenceId), elem)
        } yield msg foreach replayCallback
    }

    for {
      seqNrs <- riak fetchSequenceNrs (PersistId(persistenceId), isInRange, Some(SeqNr(max)))
      msg <- replayCallbacks(seqNrs)
    } yield ()
  }
}
