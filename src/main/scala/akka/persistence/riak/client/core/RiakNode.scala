package akka.persistence.riak.client.core
import com.basho.riak.client.core.RiakNode.{ Builder => JBuilder }
import com.basho.riak.client.core.{ RiakNode => JRiakNode }
import scala.collection.JavaConverters._

class RiakNode(val node: JRiakNode) {

}

object RiakNode {

  def apply(node: JRiakNode) = new RiakNode(node)

  class Builder(private val builder: JBuilder) {
    def withMinConnections(c: Int): Builder = {
      builder.withMinConnections(c)
      this
    }

    def withMaxConnections(c: Int): Builder = {
      builder.withMaxConnections(c)
      this
    }

    def build = RiakNode(builder.build())

    def buildNodes(nodes: List[String]) = JBuilder.buildNodes(builder, nodes.asJava).asScala
      .toList.map(RiakNode.apply)

  }

  object Builder {
    def apply() = new Builder(new JBuilder())
  }
}
