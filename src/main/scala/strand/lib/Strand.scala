package strand.lib

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors, ThreadFactory}
import scala.async.Async
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}

class Strand(globalExecutor: ExecutorService) extends Closeable:
  // Single threaded executor but used a virtual thread
  // Hence, theoretically any number of Strands can be created in user space
  private val strandExecutor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory())
  private val strandEc: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(strandExecutor)
  private val globalEc: ExecutionContext                = ExecutionContext.fromExecutorService(globalExecutor)

  inline def async[T](inline x: T): Future[T]     = Async.async(x)(using strandEc)
  extension [T](x: Future[T]) protected inline def await: T = Async.await(x)(using strandEc)
  protected inline def block[T](inline blockingOp: T): T    = Async.async(blockingOp)(using globalEc).await

  def close(): Unit = strandExecutor.shutdown()
