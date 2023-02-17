package strand.lib

import common.Cancellable
import common.RichExecutor.async

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, ThreadFactory}
import scala.async.Async
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.jdk.FutureConverters.CompletionStageOps

trait Context:
  def executionContext: ExecutionContext
  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable
  def stop(): Unit

class Strand(protected val context: Context):
  given ExecutionContext = context.executionContext

  inline def async[T](inline x: T): Future[T]               = Async.async(x)
  extension [T](x: Future[T]) protected inline def await: T = Async.await(x)

class StrandSystem:
  val globalExecutor         = Executors.newVirtualThreadPerTaskExecutor()
  private val strandExecutor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())
  given ExecutionContext     = ExecutionContext.fromExecutorService(strandExecutor)

  private var children: List[Context] = Nil

  def async[T](op: => T): Future[T] = globalExecutor.async(op)

  def spawn[T <: Strand](strandFactory: Context => T): T =
    val ctx = ContextImpl(strandFactory)
    Future:
      children ::= ctx
    ctx.self

  def stop(): Future[Unit] = Future:
    children.foreach(_.stop())
    globalExecutor.shutdown()
    strandExecutor.shutdown()

private class ContextImpl[T](strandFactory: Context => T) extends Context:
  private val strandExecutor =
    Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(strandExecutor)

  val self: T = strandFactory(this)

  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable =
    val future = strandExecutor.schedule[Unit](() => action, delay.length, delay.unit)
    () => future.cancel(true)

  def stop(): Unit =
    strandExecutor.shutdown()
