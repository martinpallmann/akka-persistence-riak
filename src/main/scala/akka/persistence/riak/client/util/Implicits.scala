package akka.persistence.riak.client.util

import com.basho.riak.client.core.{ RiakFutureListener, RiakFuture }

import scala.concurrent.{ ExecutionContext, Promise, Future }
import scala.util.{ Try, Failure, Success }

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
  implicit class FutureAwareTry[A](tr: Try[A]) {
    def asFuture: Future[A] = tr match {
      case Success(v) => Future.successful(v)
      case Failure(t) => Future.failed(t)
    }
  }
}
