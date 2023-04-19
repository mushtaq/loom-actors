package common

import java.util.concurrent.{CompletableFuture, ExecutorService}
import scala.concurrent.Future
import scala.jdk.FutureConverters.CompletionStageOps

object RichExecutor:
  extension (executor: ExecutorService)
    def async[T](op: => T): Future[T] =
      CompletableFuture
        .supplyAsync(() => op, executor)
        .asScala
