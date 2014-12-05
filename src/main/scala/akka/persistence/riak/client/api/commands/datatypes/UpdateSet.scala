package akka.persistence.riak.client.api.commands.datatypes

import akka.persistence.riak.Riak.Location
import akka.persistence.riak.client.core.query.RiakObject
import com.basho.riak.client.api.commands.datatypes.SetUpdate
import com.basho.riak.client.api.commands.datatypes.UpdateSet.{ Builder => JBuilder }

object UpdateSet {
  class Builder(private val builder: JBuilder) {
    def build = builder.build
  }

  object Builder {
    def apply(loc: Location, up: SetUpdate) = new Builder(new JBuilder(loc.asJava, up))
  }
}
