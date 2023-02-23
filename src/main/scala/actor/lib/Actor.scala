package actor.lib

import common.Cancellable
import common.RichExecutor.async
import strand.lib.Strand

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, ThreadFactory}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future, Promise}

trait ActorRef[-T]:
  def send(message: T): Unit
  def ask[R](f: Promise[R] => T): Future[R]
  def stop(): Unit

trait Context[-T]:
  def executionContext: ExecutionContext
  def self: ActorRef[T]
  def spawn[R](actorFactory: Context[R] ?=> Actor[R]): ActorRef[R]
  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable
  def stop(): Unit

abstract class Actor[-T](using protected val context: Context[T]):
  given ExecutionContext = context.executionContext
  def receive(message: T): Unit

object Actor:
  def Empty: Actor[Unit] = new Actor[Unit](using null):
    override def receive(message: Unit): Unit = ()

private class ContextImpl[T](actorFactory: Context[T] ?=> Actor[T]) extends Context[T]:
  private var children: List[ActorRef[_]] = Nil

  private val strandExecutor =
    Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(strandExecutor)

  given ExecutionContext = executionContext

  val self: ActorRef[T] = ActorRefImpl[T](actorFactory)(using this)

  def spawn[R](actorFactory: Context[R] ?=> Actor[R]): ActorRef[R] =
    val ref = ContextImpl[R](actorFactory).self
    Future:
      children ::= ref
    ref

  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable =
    val value = strandExecutor.schedule[Unit](() => action, delay.length, delay.unit)
    () => value.cancel(true)

  def stop(): Unit =
    children.foreach(_.stop())
    strandExecutor.shutdown()

private class ActorRefImpl[T](actorFactory: Context[T] ?=> Actor[T])(using context: Context[T]) extends ActorRef[T]:

  private val actor = actorFactory

  def send(message: T): Unit =
    Future(actor.receive(message))(using context.executionContext)

  def ask[R](f: Promise[R] => T): Future[R] =
    val p = Promise[R]()
    send(f(p))
    p.future

  def stop(): Unit = context.stop()

class ActorSystem extends ContextImpl[Unit](x ?=> Actor.Empty):
  val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

  def async[T](op: => T): Future[T] = globalExecutor.async(op)

  override def stop(): Unit = Future:
    globalExecutor.shutdown
    super.stop()
