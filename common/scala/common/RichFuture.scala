package common

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object RichFuture:
  extension [T](x: Future[T]) def block(): T = Await.result(x, 10.seconds)
