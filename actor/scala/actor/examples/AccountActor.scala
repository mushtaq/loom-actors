package actor.examples

import actor.examples.AccountActor.{Deposit, Get, Msg}
import actor.lib.{Actor, ActorSystem, Context}
import common.RichFuture.block

import scala.async.Async.*
import scala.concurrent.Promise

//-----------------------------------------------------------------------------------------
object AccountActor:
  sealed trait Msg
  case class Get(response: Promise[Int])                  extends Msg
  case class Deposit(value: Int, response: Promise[Unit]) extends Msg

//-----------------------------------------------------------------------------------------
class AccountActor(using Context[Msg]) extends Actor[Msg]:
  private var balance = 0
  override def receive(message: Msg): Unit = message match
    case Get(response) =>
      response.trySuccess(balance)
    case Deposit(value, response) =>
      balance += value
      response.trySuccess(())

//=========================================================================================
@main
def accountMain(): Unit =
  val system       = ActorSystem()
  val accountActor = system.spawn(AccountActor())
  println(accountActor.ask(p => Get(p)).block())

  (1 to 10000)
    .map(* => accountActor.ask(Deposit(1, _)))
    .foreach(_.block())

  val result = accountActor.ask(p => Get(p)).block()
  println(result)

  system.stop().block()
