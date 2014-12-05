package akka.persistence.riak.client.api

import akka.persistence.riak.Riak.Location
import akka.persistence.riak.client.api.commands.kv.DeleteValue
import akka.persistence.riak.client.core.RiakCluster
import akka.persistence.riak.client.util.Implicits._
import com.basho.riak.client.api.{ RiakClient => JRiakClient, RiakCommand }

object RiakClient {
  def apply(cluster: RiakCluster) = new RiakClient(new JRiakClient(cluster.cluster))
}

class RiakClient(client: JRiakClient) {

  def delete(loc: Location) = this execute DeleteValue.Builder(loc).build

  def execute[T, C](cmd: RiakCommand[T, C]) = (client executeAsync cmd).asScalaFuture
}
