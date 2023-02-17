package strand.examples

import common.ExternalService
import common.RichExecutor.async
import common.RichFuture.block
import strand.lib.{Context, Strand, StrandSystem}

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

// All methods can be scheduled on a single thread ('the Strand')
// Direct mutating operation on the shared state is allowed
// Blocking calls and Future results must be handled via the 'StrandContext'
class Account(externalService: ExternalService, context: Context) extends Strand(context):
  private var balance = 0
  private var totalTx = 0

  private val interestRate = 0.10

  def get(): Future[Int] = async:
    totalTx += 1
    balance

  def set(x: Int): Future[Unit] = async:
    // User can freely mutate the shared variables because all ops are scheduled on 'the Strand'
    totalTx += 1
    externalService.ioCall().await
    balance += x

  def computeInterest(): Future[Double] = async:
    balance * interestRate

  def getBalanceWithInterest(): Future[Double] = async:
    totalTx += 1
    balance + computeInterest().await

object Account:
  @main def accountMain: Unit =

    val system  = new StrandSystem
    val account = system.spawn(ctx => new Account(ExternalService(system.globalExecutor), ctx))

    println(account.getBalanceWithInterest().block())

    val accResult = test(account, system) // some Acc updates are lost

    println(s"accResult = $accResult")

    system.stop()

  private def test(acc: Account, system: StrandSystem) =
    // Asynchronously increments the balance by 1
    def update(): Future[Unit] =
      system.async:
        acc.set(1).block()

    // Large number of concurrent updates
    val updateFutures: Seq[Future[Unit]] = (1 to 1000).map(* => update())

    // Wait for all updates to finish
    updateFutures.foreach(_.block())

    // Read the current balance
    val result = acc.get().block()

    result
