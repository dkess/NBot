package nbot

import akka.actor.{Actor, ActorRef}

class Factoids extends Actor with CommandDelegator[String,String] {
  val delegates = Seq(FactoidFile, RandomFactoid)

  // TODO: make this code less repetitive
  def receive = {
    case Privmsg(nick, target, msg) if { val smsg = msg.split(' '); smsg(0).equals("!give") && smsg.length >= 3} =>
      val smsg = msg.split(" ",3)
      val toSend = if (target(0) == '#' | target(0) == '&') target else nick
      runCmd(smsg(2)) foreach {
        sender ! s"PRIVMSG $toSend :${smsg(1)}: "+_
      }

    case Privmsg(nick, target, msg) if msg(0) == '!' =>
      val toSend = if (target(0) == '#' | target(0) == '&') target else nick
      runCmd(msg.substring(1)) foreach {
        sender ! s"PRIVMSG $toSend :"+_
      }

    case _ =>
  }
}
