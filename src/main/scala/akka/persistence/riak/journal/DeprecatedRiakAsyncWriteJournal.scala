package akka.persistence.journal.riak

import akka.persistence.{ PersistentRepr, PersistentConfirmation, PersistentId }
import akka.persistence.journal.AsyncWriteJournal

import scala.collection.immutable.Seq
import scala.concurrent.Future

trait DeprecatedRiakAsyncWriteJournal extends AsyncWriteJournal {
  self: RiakAsyncWriteJournalOld =>

  @deprecated("writeConfirmations will be removed, since Channels will be removed.", since = "0.0")
  override def asyncWriteConfirmations(confirmations: Seq[PersistentConfirmation]): Future[Unit] = ???

  @deprecated("asyncDeleteMessages will be removed.", since = "0.0")
  override def asyncDeleteMessages(messageIds: Seq[PersistentId], permanent: Boolean): Future[Unit] = ???
}
