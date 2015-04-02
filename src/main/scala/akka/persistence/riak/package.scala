package akka.persistence

import com.basho.riak.client.core.query.{ Namespace, Location => JLocation }
import com.basho.riak.client.core.util.BinaryValue

package object riak {

  case class PersistId(value: String) extends AnyVal {
    def toBinaryValue = BinaryValue createFromUtf8 value
  }

  object SeqNr {
    def apply(value: BinaryValue): SeqNr = SeqNr(value.toStringUtf8.toLong)
  }

  case class SeqNr(value: Long) extends AnyVal {
    def <(other: SeqNr) = value < other.value
    def <=(other: SeqNr) = value <= other.value
    def >(other: SeqNr) = value > other.value
    def >=(other: SeqNr) = value >= other.value
    def toBinaryValue = BinaryValue createFromUtf8 value.toString
  }

  case class JournalBucketType(value: String) extends AnyVal {
    def toBinaryValue = BinaryValue createFromUtf8 value
  }

  sealed trait DeleteMode
  case object Permanent extends DeleteMode
  case object Temporary extends DeleteMode

  val alwaysTrue: (Any => Boolean) = _ => true

  object Location {

    private def location(bucketType: String, bucketName: String, key: String) = new JLocation(
      new Namespace(BinaryValue createFromUtf8 bucketType, BinaryValue createFromUtf8 bucketName),
      BinaryValue createFromUtf8 key
    )

    def apply(pId: PersistId, seqNr: SeqNr)(implicit bucketType: JournalBucketType): JLocation =
      location(bucketType.value, pId.value, seqNr.value.toString)

    def apply(pId: PersistId)(implicit bucketType: JournalBucketType): JLocation =
      location("journal-seqnrs", "journal-seqnrs", pId.value)

    def apply(m: PersistentRepr)(implicit bucketType: JournalBucketType): JLocation = {
      location(bucketType.value, m.persistenceId, m.sequenceNr.toString)
    }
  }
}
