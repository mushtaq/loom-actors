package actor.lib

import strand.lib.Strand
import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, ThreadFactory}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future, Promise}

trait ActorRef[-T]:
  def send(message: T): Unit
  def ask[R](f: Promise[R] => T): Future[R]
  def shutdown(): Unit

trait Context[T]:
  def executionContext: ExecutionContext
  def self: ActorRef[T]
  def shutdown(): Unit

abstract class Actor[-T](context: Context[T]):
  given ExecutionContext = context.executionContext
  def receive(message: T): Unit

object Actor:
  def spawn[T](actorFactory: Context[T] => Actor[T]): ActorRef[T] =
    ContextImpl[T](actorFactory).self

private class ContextImpl[T](actorFactory: Context[T] => Actor[T]) extends Context[T]:
  private val strandExecutor =
    Executors.newSingleThreadExecutor(Thread.ofVirtual().factory())

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(strandExecutor)

  val self: ActorRef[T] =
    new ActorRefImpl[T](actorFactory, this)(using executionContext)

  def shutdown(): Unit =
    strandExecutor.shutdown()

private class ActorRefImpl[T](actorFactory: Context[T] => Actor[T], context: Context[T])(using ExecutionContext)
    extends ActorRef[T]:

  private val actor = actorFactory(context)

  def send(message: T): Unit =
    Future(actor.receive(message))

  def ask[R](f: Promise[R] => T): Future[R] =
    val p = Promise[R]()
    send(f(p))
    p.future

  def shutdown(): Unit = context.shutdown()
