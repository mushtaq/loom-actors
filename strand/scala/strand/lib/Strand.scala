package strand.lib

import common.Cancellable
import common.RichExecutor.async

import java.util.concurrent.Executors
import scala.async.Async
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait Context:
  def executionContext: ExecutionContext
  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable
  def spawn[T <: Strand](strandFactory: Context ?=> T): T
  def stop(): Unit

class Strand(using protected val context: Context):
  given ExecutionContext = context.executionContext

  inline def async[T](inline x: T): Future[T]               = Async.async(x)
  extension [T](x: Future[T]) protected inline def await: T = Async.await(x)

private class ContextImpl[T](strandFactory: Context ?=> T) extends Context:
  private var children: List[Context] = Nil

  private val strandExecutor =
    Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())

  val executionContext: ExecutionContext =
    ExecutionContext.fromExecutorService(strandExecutor)

  given ExecutionContext = executionContext

  val self: T = strandFactory(using this)

  def spawn[R <: Strand](strandFactory: Context ?=> R): R =
    val ctx = ContextImpl(strandFactory)
    Future:
      children ::= ctx
    ctx.self

  def schedule(delay: FiniteDuration)(action: => Unit): Cancellable =
    val future = strandExecutor.schedule[Unit](() => action, delay.length, delay.unit)
    () => future.cancel(false)

  def stop(): Unit =
    children.foreach(_.stop())
    strandExecutor.shutdown()

class StrandSystem extends ContextImpl(_ ?=> ()):
  val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

  def async[T](op: => T): Future[T] = globalExecutor.async(op)

  override def stop(): Unit = Future:
    globalExecutor.shutdown()
    super.stop()
