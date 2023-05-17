package actor.lib

import common.Cancellable

import java.util.concurrent.Executors
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

trait ActorRef[-T]:
  def send(message: T): Unit
  def ask[R](f: Promise[R] => T): Future[R]
  def stop(): Future[Unit]

//-----------------------------------------------------------------------------------------
private class ActorRefImpl[T](actor: Actor[T])(using context: Context[T]) extends ActorRef[T]:
  def send(message: T): Unit =
    Future(actor.receive(message))(using context.executionContext)

  def ask[R](f: Promise[R] => T): Future[R] =
    val p = Promise[R]()
    send(f(p))
    p.future

  def stop(): Future[Unit] = context.stop()

//===========================================================================================
abstract class Actor[-T](using protected val context: Context[T]):
  given ExecutionContext = context.executionContext
  def receive(message: T): Unit

//===========================================================================================
trait Context[-T]:
  def executionContext: ExecutionContext
  def self: ActorRef[T]
  def spawn[R](actorFactory: Context[R] ?=> Actor[R]): ActorRef[R]
  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable
  def stop(): Future[Unit]

//-----------------------------------------------------------------------------------------
private class ContextImpl[T](actorFactory: Context[T] ?=> Actor[T]) extends Context[T]:
  private var children: List[ActorRef[_]] = Nil

  private val strandExecutor             = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
  val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(strandExecutor)

  given ExecutionContext = executionContext

  val self: ActorRef[T] = ActorRefImpl[T](actorFactory(using this))(using this)

  def spawn[R](actorFactory: Context[R] ?=> Actor[R]): ActorRef[R] =
    val ref = ContextImpl[R](actorFactory).self
    Future:
      children ::= ref
    ref

  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable =
    val value = strandExecutor.schedule[Unit](() => action, delay.length, delay.unit)
    () => value.cancel(true)

  def stop(): Future[Unit] =
    Future
      .traverse(children)(_.stop())
      .map(_ => strandExecutor.shutdown())

//===========================================================================================
class ActorSystem:
  private val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()
  given ExecutionContext     = ExecutionContext.fromExecutorService(globalExecutor)

  private val context: ContextImpl[Unit] = ContextImpl[Unit](_ ?=> null)
  export context.{spawn, schedule}

  def stop(): Future[Unit]           = context.stop().map(_ => globalExecutor.shutdown())
  def future[T](op: => T): Future[T] = Future(op)
