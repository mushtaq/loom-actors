package common

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.Future
import scala.jdk.FutureConverters.CompletionStageOps

object RichExecutor:
  extension (executor: ExecutorService)
    def async[T](op: => T): Future[T] =
      CompletableFuture
        .supplyAsync(() => op, executor)
        .asScala
