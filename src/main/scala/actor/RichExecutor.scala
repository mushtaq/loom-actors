package actor

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}

object RichExecutor:
  extension (executor: ExecutorService)
    def async[T](op: => T): CompletableFuture[T] = CompletableFuture.supplyAsync(
      () => op,
      executor
    )
