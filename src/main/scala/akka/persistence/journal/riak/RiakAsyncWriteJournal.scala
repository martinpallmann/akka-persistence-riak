package akka.persistence.journal.riak

import akka.persistence.PersistentRepr
import akka.persistence.journal.AsyncWriteJournal

import scala.collection.immutable.Seq
import scala.concurrent.Future

class RiakAsyncWriteJournal extends AsyncWriteJournal {
  override def asyncWriteMessages(messages: Seq[PersistentRepr]): Future[Unit] =
    Future.successful(Unit)

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long, permanent: Boolean): Future[Unit] =
    Future.successful(Unit)

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] =
    Future.successful(-1L)

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: (PersistentRepr) => Unit): Future[Unit] =
    Future.successful(Unit)

  // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //                                                                                                                  //
  // DEPRECATED STUFF BELOW THIS POINT                                                                                //
  //                                                                                                                  //
  // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  import akka.persistence.{ PersistentConfirmation, PersistentId }

  @deprecated("writeConfirmations will be removed, since Channels will be removed.", since = "0.1")
  override def asyncWriteConfirmations(confirmations: Seq[PersistentConfirmation]): Future[Unit] =
    Future.successful(Unit)

  @deprecated("asyncDeleteMessages will be removed.", since = "0.1")
  override def asyncDeleteMessages(messageIds: Seq[PersistentId], permanent: Boolean): Future[Unit] =
    Future.successful(Unit)
}
