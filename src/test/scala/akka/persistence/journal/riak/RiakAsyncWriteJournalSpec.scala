package akka.persistence.journal.riak

import akka.persistence.journal.JournalSpec
import com.typesafe.config.{ Config, ConfigFactory }

class RiakAsyncWriteJournalSpec extends JournalSpec {
  override lazy val config: Config = ConfigFactory.load(this.getClass.getSimpleName)
}
