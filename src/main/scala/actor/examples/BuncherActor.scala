package actor.examples

import actor.examples.BuncherActor.{Info, Msg, Timeout}
import actor.examples.BuncherDestinationActor.Batch
import actor.lib.{Actor, ActorRef, ActorSystem, Context}
import common.Cancellable

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class BuncherDestinationActor(using Context[Batch]) extends Actor[Batch]:
  override def receive(message: Batch): Unit =
    println(s"Got batch of ${message.messages.size} messages: ${message.messages.mkString(", ")} ")

object BuncherDestinationActor:
  case class Batch(messages: Vector[Msg])

class BuncherActor(target: ActorRef[Batch], after: FiniteDuration, maxSize: Int)(using Context[Msg]) extends Actor[Msg]:
  private var isIdle: Boolean     = true
  private var buffer: Vector[Msg] = Vector.empty
  private var timer: Cancellable  = () => true

  override def receive(message: Msg): Unit =
    if isIdle then idle(message) else active(message)

  private def idle(message: Msg): Unit =
    buffer :+= message
    timer = context.schedule(after):
      context.self.send(Timeout)
    isIdle = false

  private def active(message: Msg): Unit = message match
    case Timeout =>
      sendBatchAndIdle()
    case Info(msg) =>
      buffer :+= message
      if buffer.size == maxSize then
        sendBatchAndIdle()
        timer.cancel()

  private def sendBatchAndIdle(): Unit =
    target.send(Batch(buffer))
    buffer = Vector.empty
    isIdle = true

object BuncherActor:
  sealed trait Msg
  case class Info(message: String) extends Msg
  private case object Timeout      extends Msg

class BuncherTestActor(using Context[Unit]) extends Actor[Unit]:
  override def receive(message: Unit): Unit = ()

  private val target: ActorRef[Batch] = context.spawn(BuncherDestinationActor())
  private val buncher: ActorRef[Msg]  = context.spawn(BuncherActor(target, 3.seconds, 10))

  (1 to 15).foreach: x =>
    buncher.send(Info(x.toString))

  context.schedule(1.seconds):
    buncher.send(Info("16"))

  context.schedule(2.seconds):
    buncher.send(Info("17"))

  context.schedule(4.seconds):
    buncher.send(Info("18"))

object BuncherTestActor:
  @main
  def buncherApp: Unit =
    val system = ActorSystem()
    system.spawn(BuncherTestActor())
  //    StdIn.readLine()
  //    system.stop()
