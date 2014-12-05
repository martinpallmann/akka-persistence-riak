package akka.persistence.riak

import com.basho.riak.client.core.util.BinaryValue

import scala.collection.IterableLike
import scala.collection.immutable.Seq
import scala.concurrent.{ ExecutionContext, Future }

object Implicits {

  implicit class RiakAwareString(s: String) {
    val binary = BinaryValue createFromUtf8 s
    val bucketName = Riak.Bucket.Name(BinaryValue createFromUtf8 s)
    val bucketType = Riak.Bucket.Type(BinaryValue createFromUtf8 s)
    val key = Riak.Key(s)
  }

  implicit class RiakAwareLong(l: Long) {
    val key = Riak.Key(l.toString)
  }

  implicit class EnhancedIterableLike[+A, +Repr](i: IterableLike[A, Repr]) {
    def takeL(n: Long): Repr = {
      var count = 0L
      i.takeWhile(_ => { count += 1L; count <= n })
    }
  }

  implicit def convertFuture(f: Future[_])(implicit ec: ExecutionContext): Future[Unit] = f.map(_ => ())
}
