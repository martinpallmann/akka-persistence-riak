package akka.persistence.riak.client.api.commands.datatypes

import akka.persistence.riak.Riak.Location
import com.basho.riak.client.api.commands.datatypes.FetchSet.{ Builder => JBuilder }

object FetchSet {
  class Builder(private val builder: JBuilder) {
    def build = builder.build
  }

  object Builder {
    def apply(l: Location) = new Builder(new JBuilder(l.asJava))
  }
}
