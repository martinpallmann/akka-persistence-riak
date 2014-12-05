package akka.persistence.riak.client.api.commands.kv

import akka.persistence.riak.Riak.Location
import com.basho.riak.client.api.commands.kv.FetchValue.{ Builder => JBuilder }

object FetchValue {
  class Builder(private val builder: JBuilder) {
    def build = builder.build
  }

  object Builder {
    def apply(l: Location) = new Builder(new JBuilder(l.asJava))
  }
}
