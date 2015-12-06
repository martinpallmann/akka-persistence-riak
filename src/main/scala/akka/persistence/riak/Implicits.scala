package akka.persistence.riak

import com.basho.riak.client.core.{ RiakFutureListener, RiakFuture }
import com.typesafe.config.{ ConfigException, Config }
import scala.collection.IterableLike
import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.util.Try
import scala.language.implicitConversions

object Implicits {

  implicit class ScalaAwareRiakFuture[A, B](rf: RiakFuture[A, B]) {
    def asScalaFuture: Future[A] = {
      val p = Promise[A]()
      val l = new RiakFutureListener[A, B] {
        override def handle(f: RiakFuture[A, B]) = {
          if (f.isSuccess) { p.success(f.get()) }
          else { p.failure(f.cause()) }
          rf.removeListener(this)
        }
      }
      rf.addListener(l)
      p.future
    }
  }

  implicit class EnhancedIterableLike[+A, +Repr](i: IterableLike[A, Repr]) {
    def take(n: SeqNr): Repr = {
      var count = 0L
      i.takeWhile(_ => { count += 1L; count <= n.value })
    }
  }

  implicit def toUnitFuture(f: Future[_])(implicit ec: ExecutionContext): Future[Unit] = f map (_ => ())

  implicit class EnhancedConfig(c: Config) {
    def getStringOpt(path: String): Option[String] = (Try { Option(c.getString(path)) } recover {
      case _: ConfigException.Missing => None
    }).get

    def getString(path: String, default: String) = getStringOpt(path) getOrElse default

  }
}
