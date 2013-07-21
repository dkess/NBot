package nbot

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Random
import ExecutionContext.Implicits.global

class Mfk extends Actor {
  private def takeRandRecurse[A] (chosen:Seq[A], needed:Int, left:Int, i:Iterator[A], r:Random):Seq[A] =
    if (needed > 0) {
      if (needed > left) {
        i.toSeq
      } else {
        if (r.nextInt(left) < needed) {
          takeRandRecurse(chosen.+:(i.next), needed-1, left-1, i, r)
        } else {
          i.next
          takeRandRecurse(chosen, needed, left-1, i, r)
        }
      }
    } else {
      chosen
    }

  // take n random elements from l
  def takeRand[A] (n:Int, l:Seq[A]):Seq[A] =
    takeRandRecurse(Seq[A](), n, l.size, l.iterator, new Random())

  // insert c in string s at index i
  def strInsert(s:String, c:String, i:Int) =
    s.substring(0,i) + c + s.substring(i)

  def receive = {
    case Privmsg(nick, target, msg) if msg.startsWith("!mfk") =>
      val ircclient = sender
      ask(context.actorFor("../nicklist"),FetchResults(target, msg.substring(4).trim))(5 seconds) onSuccess {
        case x:Set[String] =>
          ircclient ! s"PRIVMSG $target :MFK: "+takeRand(3, x.toSeq).map(strInsert(_,"_",1)).mkString(" ")
        case _ => {}
      }

    case _ => {}
  }
}
