package actor

import actor.AccountActor.{Deposit, Get, Msg}

import scala.concurrent.{Future, Promise}

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
