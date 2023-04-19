package common

import common.RichExecutor.async

import java.util.concurrent.ExecutorService
import scala.concurrent.Future

class ExternalService(globalExecutor: ExecutorService) {
  // Demo IO call to an external service that takes a few millis to complete
  def ioCall(): Future[Int] =
    globalExecutor.async:
      Thread.sleep(10)
      99
}
