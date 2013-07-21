package nbot

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

class Callops extends Actor {
  var lastUsed = Map[String,Long]()
  val cooldown = 300000000000L // 5 minutes
  def receive = {
    case Privmsg(nick, chan, msg) if msg.startsWith("!callops") =>
      val time = System.nanoTime
      if (lastUsed.get(chan).map((time - cooldown) < _).getOrElse(false)) {
        sender ! s"PRIVMSG $chan :You must wait before using this command again!"
      } else {
        lastUsed = lastUsed + (chan -> time)
        val ircclient = sender
        ask(context.actorFor("../nicklist"),FetchResults(chan, "ops"))(5 seconds) onSuccess {
          case x:Set[String] =>
            ircclient ! s"PRIVMSG $chan :$nick uses !callops: "+x.mkString(" ")
        }
      }
    case _ => {}
  }
}

        
