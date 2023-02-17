package actor.lib

import common.Cancellable
import strand.lib.Strand

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, ThreadFactory}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future, Promise}

trait ActorRef[-T]:
  def send(message: T): Unit
  def ask[R](f: Promise[R] => T): Future[R]
  def stop(): Unit

trait Context[T]:
  def executionContext: ExecutionContext
  def self: ActorRef[T]
  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable
  def stop(): Unit

abstract class Actor[-T](context: Context[T]):
  given ExecutionContext = context.executionContext
  def receive(message: T): Unit

object Actor:
  def spawn[T](actorFactory: Context[T] => Actor[T]): ActorRef[T] =
    ContextImpl[T](actorFactory).self

private class ContextImpl[T](actorFactory: Context[T] => Actor[T]) extends Context[T]:
  private val strandExecutor =
    Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(strandExecutor)

  val self: ActorRef[T] =
    new ActorRefImpl[T](actorFactory, this)

  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable =
    val value = strandExecutor.schedule[Unit](() => action, delay.length, delay.unit)
    () => value.cancel(true)

  def stop(): Unit =
    strandExecutor.shutdown()

private class ActorRefImpl[T](actorFactory: Context[T] => Actor[T], context: Context[T]) extends ActorRef[T]:

  private val actor = actorFactory(context)

  def send(message: T): Unit =
    Future(actor.receive(message))(using context.executionContext)

  def ask[R](f: Promise[R] => T): Future[R] =
    val p = Promise[R]()
    send(f(p))
    p.future

  def stop(): Unit = context.stop()
