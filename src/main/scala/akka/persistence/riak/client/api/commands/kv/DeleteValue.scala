package akka.persistence.riak.client.api.commands.kv

import akka.persistence.riak.Riak.Location
import akka.persistence.riak.client.core.query.RiakObject
import com.basho.riak.client.api.commands.kv.DeleteValue.{ Builder => JBuilder }

object DeleteValue {
  class Builder(private val builder: JBuilder) {
    def build = builder.build
  }

  object Builder {
    def apply(l: Location) = new Builder(new JBuilder(l.asJava))
  }
}
