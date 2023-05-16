//package callback.examples
//
//import AccountActor.{Deposit, Get, Msg}
//import callback.lib.{Actor, ActorSystem, Context}
//import common.RichFuture.block
//
//import scala.concurrent.{Future, Promise}
//import async.Async.*
//
//class AccountActor(using Context[Msg]) extends Actor[Msg]:
//  private var balance = 0
//
//  override def behavior: Behavior = Behavior:
//    case Get(response) =>
//      async:
//        await(Future.successful(10))
//        context.self.send(Deposit(0, Promise()))
//      //      Future
//      //        .successful(10)
//      //        .onComplete(_ => context.self.send(Deposit(0, Promise())))
//      response.trySuccess(balance)
//    case Deposit(value, response) =>
//      balance += value
//      response.trySuccess(())
//
//object AccountActor:
//  sealed trait Msg
//  case class Get(response: Promise[Int])                  extends Msg
//  case class Deposit(value: Int, response: Promise[Unit]) extends Msg
//
//  /////////////////////////
//
//  @main
//  def accountActorMain(): Unit =
//    val system       = ActorSystem()
//    val accountActor = system.spawn(AccountActor())
//    println(accountActor.ask(p => Get(p)).block())
//
//    def update(): Future[Unit] =
//      system
//        .async:
//          accountActor.ask(Deposit(1, _))
//        .block()
//
//    (1 to 1000)
//      .map(* => update())
//      .foreach(_.block())
//
//    val result = accountActor.ask(p => Get(p)).block()
//    println(result)
//
//    system.stop()
