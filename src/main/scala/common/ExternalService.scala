package common

import common.RichExecutor.async

import java.util.concurrent.{CompletableFuture, ExecutorService}
import scala.concurrent.Future
import scala.jdk.FutureConverters.CompletionStageOps

class ExternalService(globalExecutor: ExecutorService) {
  // Demo IO call to an external service that takes a few millis to complete
  def ioCall(): Future[Int] =
    globalExecutor.async:
      Thread.sleep(10)
      99

}
