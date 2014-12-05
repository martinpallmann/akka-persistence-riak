package akka.persistence.journal.riak

import akka.persistence.PersistentRepr
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.riak.Riak
import akka.persistence.riak.Riak._
import akka.serialization.{ SerializationExtension, Serialization }
import akka.persistence.riak.Implicits._
import com.basho.riak.client.api.commands.datatypes.SetUpdate
import com.basho.riak.client.core.util.BinaryValue
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.collection.JavaConverters._

class RiakAsyncWriteJournal extends AsyncWriteJournal {

  private lazy val journalBucketType = "journal".bucketType // TODO configurable
  private lazy val journalSeqNrsBucketType = "journal-seqnrs".bucketType // TODO configurable

  override def asyncWriteMessages(messages: Seq[PersistentRepr]): Future[Unit] = {

    def storeMessage(m: PersistentRepr): Future[(String, Long)] =
      Riak.store(dataLoc(m), this serialize m) map (_ => (m.persistenceId, m.sequenceNr))

    def storeSeqNrs(seqNrs: Seq[(String, Long)]) = seqNrs.groupBy(_._1).map {
      case (persistenceId, v) =>
        val up = new SetUpdate()
        v.map(_._2).foreach(sn => up.add(BinaryValue.create(sn.toString)))
        Riak.updateSet(seqNrLoc(persistenceId), up)
    }

    Future.sequence { messages map storeMessage } map storeSeqNrs
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long, permanent: Boolean): Future[Unit] = {

    def delete: (String, Long) => Future[Unit] = permanent match {
      case true => (pId: String, sNr: Long) => Riak.delete(dataLoc(pId, sNr))
      case false => (pId: String, sNr: Long) => Riak.delete(dataLoc(pId, sNr))
    }

    for {
      seqNrs <- fetchSequenceNrs(persistenceId, _ <= toSequenceNr)
      _ <- Future.sequence { seqNrs.map(seqNr => delete(persistenceId, seqNr)) }
    } yield { () }
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] =
    fetchSequenceNrs(persistenceId).map {
      case xs if xs.isEmpty => 0L
      case xs               => xs.max
    }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: (PersistentRepr) => Unit): Future[Unit] = {

    def inRange(l: Long) = l >= fromSequenceNr && l <= toSequenceNr

    def fetchValue(seqNr: Long): Future[Option[PersistentRepr]] = Riak.fetch(dataLoc(persistenceId, seqNr))
      .map(_.getValues.asScala.headOption.flatMap(r =>
        if (r.getUserMeta.containsKey("deleted")) None
        else Some(deserialize(r.getValue.getValue)))
      )

    def replay(seqNrs: List[Long]): Future[Unit] = seqNrs match {
      case Nil     => Future.successful(Unit)
      case x :: xs => fetchValue(x).map(_.map(replayCallback)).flatMap(_ => replay(xs))
    }

    for {
      seqNrs <- fetchSequenceNrs(persistenceId, inRange, Some(max))
      msg <- replay(seqNrs)
    } yield ()
  }

  private def fetchSequenceNrs(persistenceId: String, filter: Long => Boolean = _ => true, max: Option[Long] = None) = {
    val result = Riak.fetchSet(seqNrLoc(persistenceId)).map(_.getDatatype.view()
      .asScala.map(_.toString.toLong)
      .filter(filter)
      .toList.sorted)
    max match {
      case Some(m) => result.map(_.takeL(m))
      case _       => result
    }
  }

  private def dataLoc(m: PersistentRepr): Location = dataLoc(m.persistenceId, m.sequenceNr)
  private def dataLoc(pId: String, seqNr: Long): Location = Location(journalBucketType, pId.bucketName, seqNr.key)
  private def seqNrLoc(persistenceId: String) = Location(journalSeqNrsBucketType, persistenceId.bucketName, "seqnrs".key)

  private val ser: Serialization = SerializationExtension(context.system)

  private def serialize(p: PersistentRepr) = ser.serializerFor(classOf[PersistentRepr]).toBinary(p)
  private def deserialize(bytes: Array[Byte]): PersistentRepr = ser.serializerFor(classOf[PersistentRepr]).fromBinary(bytes, classOf[PersistentRepr]).asInstanceOf[PersistentRepr]

  // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //                                                                                                                  //
  // DEPRECATED STUFF BEYOND THIS POINT                                                                               //
  //                                                                                                                  //
  // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  import akka.persistence.{ PersistentConfirmation, PersistentId }

  @deprecated("writeConfirmations will be removed, since Channels will be removed.", since = "0.1")
  override def asyncWriteConfirmations(confirmations: Seq[PersistentConfirmation]): Future[Unit] =
    Future.successful(Unit)

  @deprecated("asyncDeleteMessages will be removed.", since = "0.1")
  override def asyncDeleteMessages(messageIds: Seq[PersistentId], permanent: Boolean): Future[Unit] =
    Future.sequence { messageIds.map(mId => asyncDeleteMessagesTo(mId.persistenceId, Long.MaxValue, permanent)) }
}
