package actor.examples

import actor.examples.AccountActor.{Deposit, Get, Msg}
import actor.lib.{Actor, Context}
import common.RichExecutor.async
import common.RichFuture.block

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

class AccountActor(context: Context[Msg]) extends Actor[Msg](context):
  private var balance = 0
  override def receive(message: Msg): Unit = message match
    case Get(response) =>
      Future
        .successful(10)
        .onComplete(_ => context.self.send(Deposit(0, Promise())))
      response.trySuccess(balance)
    case Deposit(value, response) =>
      balance += value
      response.trySuccess(())

object AccountActor:
  sealed trait Msg
  case class Get(response: Promise[Int])                  extends Msg
  case class Deposit(value: Int, response: Promise[Unit]) extends Msg

  /////////////////////////

  @main
  def accountActorMain: Unit =
    val accountActor = Actor.spawn(ctx => new AccountActor(ctx))
    println(accountActor.ask(p => Get(p)).block())

    val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

    def update(): Future[Unit] =
      globalExecutor
        .async:
          accountActor.ask(Deposit(1, _))
        .block()

    (1 to 1000)
      .map(* => update())
      .foreach(_.block())

    val result = accountActor.ask(p => Get(p)).block()
    println(result)

    accountActor.shutdown()
