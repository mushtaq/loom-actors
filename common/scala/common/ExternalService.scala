package common

import scala.concurrent.{ExecutionContext, Future}

class ExternalService(using ExecutionContext) {
  // Demo IO call to an external service that takes a few millis to complete
  def ioCall(): Future[Int] = Future:
    Thread.sleep(10)
    99
}
