package actor

import actor.AccountActor.{Deposit, Get}
import RichExecutor.async

import java.io.Closeable
import java.util.concurrent.{CompletableFuture, ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.jdk.FutureConverters.{CompletionStageOps, FutureOps}

object Test:
  @main def main: Unit =
    val accountActor = Actor.spawn(ctx => new AccountActor(ctx))
    println(accountActor.ask(p => Get(p)).block())

    val globalExecutor = Executors.newVirtualThreadPerTaskExecutor()

    def update(): Future[Unit] = globalExecutor.async(accountActor.ask(p => Deposit(1, p)).block()).asScala

    (1 to 1000)
      .map(* => update())
      .foreach(_.block())

    val result = accountActor.ask(p => Get(p)).block()
    println(result)

    accountActor.shutdown()

  extension [T](x: Future[T]) private def block() = Await.result(x, 10.seconds)
