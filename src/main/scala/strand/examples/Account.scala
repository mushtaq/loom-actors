package strand.examples

import common.ExternalService
import common.RichExecutor.async
import common.RichFuture.block
import strand.lib.Strand

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

// All methods can be scheduled on a single thread ('the Strand')
// Direct mutating operation on the shared state is allowed
// Blocking calls and Future results must be handled via the 'StrandContext'
class Account(externalService: ExternalService, globalExecutor: ExecutorService) extends Strand(globalExecutor):
  private var balance = 0
  private var totalTx = 0

  private val interestRate = 0.10

  def get(): Future[Int] = async:
    totalTx += 1
    balance

  def set(x: Int): Future[Unit] = async:
    // User can freely mutate the shared variables because all ops are scheduled on 'the Strand'
    totalTx += 1
    balance += x
    externalService.ioCall().await
    block(Thread.sleep(100))

  def computeInterest(): Future[Double] = async:
    balance * interestRate

  def getBalanceWithInterest(): Future[Double] = async:
    totalTx += 1
    balance + computeInterest().await

object Account:
  @main def accountMain: Unit =
    val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

    val account = new Account(ExternalService(globalExecutor), globalExecutor)

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
