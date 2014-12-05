package akka.persistence.riak

import akka.persistence.riak.client.api.RiakClient
import akka.persistence.riak.client.api.commands.datatypes.{ FetchSet, UpdateSet }
import akka.persistence.riak.client.api.commands.kv.{ FetchValue, StoreValue }
import akka.persistence.riak.client.core.{ RiakCluster, RiakNode }
import akka.persistence.riak.client.core.query.RiakObject
import com.basho.riak.client.api.commands.datatypes.SetUpdate
import com.basho.riak.client.core.util.BinaryValue
import akka.persistence.riak.Implicits._

object Riak {

  object Bucket {
    case class Type(`type`: BinaryValue) extends AnyVal

    case class Name(name: BinaryValue) extends AnyVal
  }

  case class Namespace(bucketType: Bucket.Type, bucketName: Bucket.Name) {
    import com.basho.riak.client.core.query
    val asJava: query.Namespace = new query.Namespace(bucketType.`type`, bucketName.name)
  }

  case class Key(key: String) extends AnyVal

  case class Location(ns: Namespace, k: Key) {
    import com.basho.riak.client.core.query
    val asJava: query.Location = new query.Location(ns.asJava, BinaryValue createFromUtf8 k.key)
  }

  object Location {
    def apply(t: Bucket.Type, n: Bucket.Name, k: Key): Location = {
      Location(Namespace(t, n), k)
    }
  }

  private lazy val journalBucketType = Bucket.Type("journal".binary)

  private lazy val riak = {
    val addresses = "127.0.0.1" :: Nil
    val nodes = RiakNode.Builder()
      .withMinConnections(10)
      .withMaxConnections(50)
      .buildNodes(addresses)
    val cluster = RiakCluster.Builder(nodes).build
    cluster.start() // TODO cluster shutdown?
    RiakClient(cluster)
  }

  def store(l: Location, data: Array[Byte]) = riak execute (StoreValue.Builder(RiakObject(data)) location l).build

  def updateSet(l: Location, up: SetUpdate) = riak execute UpdateSet.Builder(l, up).build

  def fetch(l: Location) = riak execute FetchValue.Builder(l).build

  def fetchSet(l: Location) = riak execute FetchSet.Builder(l).build

  def delete(l: Location) = riak delete l

}
