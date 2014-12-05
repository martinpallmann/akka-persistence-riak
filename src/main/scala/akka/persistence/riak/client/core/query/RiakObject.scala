package akka.persistence.riak.client.core.query

import com.basho.riak.client.core.query.{ RiakObject => JRiakObject }
import com.basho.riak.client.core.query.UserMetadata.{ RiakUserMetadata => JRiakUserMetadata }
import com.basho.riak.client.core.util.BinaryValue
import com.basho.riak.client.core.util.BinaryValue.createFromUtf8

object RiakObject {
  def apply(data: Array[Byte]): RiakObject = {
    val ro = new JRiakObject()
    val binValue = BinaryValue create data
    ro setValue binValue
    new RiakObject(ro)
  }

  def apply(meta: Map[String, String]): RiakObject = {
    val ro = new JRiakObject()
    meta.map {
      case (key, value) => (ro getUserMeta) put (createFromUtf8(key), createFromUtf8(value))
    }
    new RiakObject(ro)
  }
}

class RiakObject(val riakObject: JRiakObject) {
  val userMeta = new RiakUserMetaData(riakObject getUserMeta)
}

class RiakUserMetaData(private val meta: JRiakUserMetadata) {
  def put(key: String, value: String) = {
    meta.put(createFromUtf8(key), createFromUtf8(value))
  }
}
