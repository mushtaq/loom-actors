package strand

import actor.ExternalService
import java.io.Closeable
import java.util.concurrent.ExecutorService
import scala.concurrent.Future

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
