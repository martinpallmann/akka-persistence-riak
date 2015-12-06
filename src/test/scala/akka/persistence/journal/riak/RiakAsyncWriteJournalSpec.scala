package akka.persistence.journal.riak

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory

class RiakAsyncWriteJournalSpec extends JournalSpec(ConfigFactory.load("RiakAsyncWriteJournalSpec")) {
  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = CapabilityFlag.off()
}
