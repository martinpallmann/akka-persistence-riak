package akka.persistence.riak.client.core

import com.basho.riak.client.core.{ RiakCluster => JRiakCluster }
import com.basho.riak.client.core.RiakCluster.{ Builder => JBuilder }
import scala.collection.JavaConverters._

class RiakCluster(private[client] val cluster: JRiakCluster) {
  def start(): Unit = cluster.start()
  def shutdown(): Unit = cluster.shutdown()
}

object RiakCluster {

  def apply(cluster: JRiakCluster) = new RiakCluster(cluster)

  class Builder(private val builder: JBuilder) {
    def build = RiakCluster(builder.build())
  }

  object Builder {
    def apply(nodes: List[RiakNode]) = new Builder(new JBuilder(nodes.map(_.node).asJava))
  }
}
