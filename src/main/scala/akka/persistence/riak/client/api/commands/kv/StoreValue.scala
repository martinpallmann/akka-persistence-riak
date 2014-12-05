package akka.persistence.riak.client.api.commands.kv

import akka.persistence.riak.Riak.Location
import akka.persistence.riak.client.core.query.RiakObject
import com.basho.riak.client.api.commands.kv.StoreValue.{ Builder => JBuilder }

object StoreValue {

  class Builder(private val builder: JBuilder) {
    def build = builder.build
    def location(location: Location) = {
      builder withLocation location.asJava
      this
    }
  }

  object Builder {
    def apply(o: RiakObject) = new Builder(new JBuilder(o.riakObject))
  }
}
