package akka.persistence.riak.client.api

import akka.persistence.riak.Implicits._
import akka.persistence.riak.client.api.RiakClient._
import akka.persistence.riak.client.core.RiakCluster
import com.basho.riak.client.api.commands.datatypes.UpdateSet.Response
import com.basho.riak.client.api.commands.datatypes._
import com.basho.riak.client.api.commands.kv.{ FetchValue, StoreValue, DeleteValue }
import com.basho.riak.client.api.{ RiakClient => JRiakClient, RiakCommand }
import com.basho.riak.client.core.query.{ RiakObject, Location }
import com.basho.riak.client.core.util.BinaryValue

import scala.collection.immutable.Iterable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

object RiakClient {
  def apply(cluster: RiakCluster) = new RiakClient(new JRiakClient(cluster.cluster))

  object SetActions {
    sealed trait UpdateAction
    case class Add(value: BinaryValue) extends UpdateAction
    case class Remove(value: BinaryValue) extends UpdateAction
  }

  object MapActions {
    sealed trait UpdateAction
    case class RemoveCounter(key: String) extends UpdateAction
    case class RemoveFlag(key: String) extends UpdateAction
    case class RemoveMap(key: String) extends UpdateAction
    case class RemoveRegister(key: String) extends UpdateAction
    case class RemoveSet(key: String) extends UpdateAction
    case class UpdateCounter(key: String, delta: Long) extends UpdateAction
    case class UpdateFlag(key: String, flag: Boolean) extends UpdateAction
    case class UpdateMap(key: String, actions: Iterable[MapActions.UpdateAction]) extends UpdateAction
    case class UpdateRegister(key: String, register: BinaryValue) extends UpdateAction
    case class UpdateSet(key: String, actions: Iterable[SetActions.UpdateAction]) extends UpdateAction
  }
}

class RiakClient(client: JRiakClient) {

  private object Execute {

    def apply[T, C](cmd: RiakCommand[T, C])(implicit ec: ExecutionContext): Future[T] =
      (client executeAsync cmd).asScalaFuture

    def fetchValue(key: Location)(implicit ec: ExecutionContext): Future[FetchValue.Response] =
      apply(new FetchValue.Builder(key).build())

    def fetchMap(key: Location)(implicit ec: ExecutionContext): Future[FetchMap.Response] =
      apply(new FetchMap.Builder(key).build())

    def fetchSet(key: Location)(implicit ec: ExecutionContext): Future[FetchSet.Response] =
      apply(new FetchSet.Builder(key).build())

    def storeValue(keyValue: (Location, RiakObject))(implicit ec: ExecutionContext): Future[StoreValue.Response] =
      keyValue match {
        case (key, value) => apply((new StoreValue.Builder(value) withLocation key).build())
      }

    def asUnit(void: Void) = ()

    def deleteValue(key: Location)(implicit ec: ExecutionContext): Future[Unit] =
      apply(new DeleteValue.Builder(key).build()) map asUnit

    def counterUpdate(delta: Long): CounterUpdate = new CounterUpdate(delta)

    def flagUpdate(flag: Boolean): FlagUpdate = new FlagUpdate(flag)

    def mapUpdate(actions: Iterable[MapActions.UpdateAction]): MapUpdate = {
      val result = new MapUpdate()
      actions foreach {
        case MapActions.RemoveCounter(key)            => result removeCounter key
        case MapActions.RemoveFlag(key)               => result removeFlag key
        case MapActions.RemoveMap(key)                => result removeMap key
        case MapActions.RemoveRegister(key)           => result removeRegister key
        case MapActions.RemoveSet(key)                => result removeSet key
        case MapActions.UpdateCounter(key, delta)     => result update (key, counterUpdate(delta))
        case MapActions.UpdateFlag(key, flag)         => result update (key, flagUpdate(flag))
        case MapActions.UpdateMap(key, act)           => result update (key, mapUpdate(act))
        case MapActions.UpdateRegister(key, register) => result update (key, registerUpdate(register))
        case MapActions.UpdateSet(key, act)           => result update (key, setUpdate(act))
      }
      result
    }

    def registerUpdate(register: BinaryValue): RegisterUpdate = new RegisterUpdate(register)

    def setUpdate(actions: Iterable[SetActions.UpdateAction]): SetUpdate = {
      val result = new SetUpdate()
      actions foreach {
        case SetActions.Add(value)    => result add value
        case SetActions.Remove(value) => result remove value
      }
      result
    }

    def updateSet(keyValue: (Location, Iterable[SetActions.UpdateAction]))(implicit ec: ExecutionContext): Future[Response] = keyValue match {
      case (key, value) => apply(new UpdateSet.Builder(key, setUpdate(value)).build())
    }

    def updateMap(keyValue: (Location, Iterable[MapActions.UpdateAction]))(implicit ec: ExecutionContext): Future[UpdateMap.Response] = keyValue match {
      case (key, value) => apply(new UpdateMap.Builder(key, mapUpdate(value)).build())
    }
  }

  def deleteValue(key: Location)(implicit ec: ExecutionContext): Future[Unit] = Execute deleteValue key

  def storeValue(keyValue: (Location, RiakObject))(implicit ec: ExecutionContext): Future[StoreValue.Response] = Execute storeValue keyValue

  def fetchMap(key: Location)(implicit ec: ExecutionContext): Future[FetchMap.Response] = Execute fetchMap key

  def fetchSet(key: Location)(implicit ec: ExecutionContext): Future[FetchSet.Response] = Execute fetchSet key

  def fetchValue(key: Location)(implicit ec: ExecutionContext): Future[FetchValue.Response] = Execute fetchValue key

  def updateSet(keyValue: (Location, Iterable[SetActions.UpdateAction]))(implicit ec: ExecutionContext) =
    Execute updateSet keyValue

  def updateMap(keyValue: (Location, Iterable[MapActions.UpdateAction]))(implicit ec: ExecutionContext) = Execute updateMap keyValue

  def execute[T, C](cmd: RiakCommand[T, C])(implicit ec: ExecutionContext): Future[T] = (client executeAsync cmd).asScalaFuture

}
