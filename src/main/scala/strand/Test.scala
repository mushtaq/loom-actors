package strand

import common.ExternalService
import common.RichExecutor.async
import common.RichFuture.block

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

object Test:
  @main def main: Unit =
    val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

    val account = new Account(ExternalService(globalExecutor), globalExecutor)

//    this will not deadlock
    println(account.getBalanceWithInterest().block())

    val accResult = test(account, globalExecutor) // some Acc updates are lost

    println(s"accResult = $accResult")

    globalExecutor.shutdown()

  private def test(acc: Account with Closeable, globalExecutor: ExecutorService) =
    // Asynchronously increments the balance by 1
    def update(): Future[Unit] =
      globalExecutor.async:
        acc.set(1).block()

    // Large number of concurrent updates
    val updateFutures: Seq[Future[Unit]] = (1 to 1000).map(* => update())

    // Wait for all updates to finish
    updateFutures.foreach(_.block())

    // Read the current balance
    val result = acc.get().block()

    acc.close()
    result
