package nbot

import akka.actor.{Actor, ActorRef}
import scala.collection.mutable

case class Memo(sender:String, target:String, msg:String)

class Tell extends Actor {
  val memos = mutable.Queue[Memo]()

  def sendMemos (target:String, ircClient:ActorRef) =
    memos.dequeueAll((m:Memo)=>target.toLowerCase.equals(m.target.toLowerCase)) foreach ((m:Memo)=>
      ircClient ! ("NOTICE "+target+" :Message from "+m.sender+": "+m.msg)
    )

  val whoisRegex = ":.* 311 .*? (.*?) .*".r
  def receive = {
    case Privmsg(mSender, _, fullmsg) if fullmsg.startsWith("!tell") =>
      fullmsg.split(" ",3) match {
        case Array(_, target, msg) =>
          memos += Memo(mSender, target, msg)
          sender ! "WHOIS "+target

        case _ => {}
      }
    
    // for debug purposes
    case Privmsg(_,_,"memos") =>
      println(memos)

    case whoisRegex(nick) =>
      sendMemos(nick, sender)

    case Join(nick, _) =>
      sendMemos(nick, sender)

    case _ => {}
  }
}
