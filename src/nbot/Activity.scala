package nbot

import akka.actor.{Actor, ActorRef}
import scala.collection.immutable.StringOps

class Activity extends Actor {
  // To store activity we use a list of pairings between nicks and last activity
  // It is sorted by last activity such that the most recent talkers are at the
  // beginning.
  // maybe this should be a mutable val
  var lastActive = Map().withDefaultValue(Seq[(String,Long)]()) : Map[String,Seq[(String,Long)]]
  
  // if a user does any of these things, it means they are active
  val re = """:([^!]+)(?:!.*)? (?:PRIVMSG|KICK|MODE|NOTICE) (\S+)(?: .*)?$$""".r
  
  def getActiveForTime(chan: String, time:Double): Seq[String] = {
    val (active, _) = lastActive(chan) span {
      case (_, t) if System.nanoTime() - t <= time*60000000000L => true
      case _ => false
    }
    // get the first part of the tuple (the nick)
    active.map(_._1)
  }

  def checkCmd(rawmsg:String) : Unit = rawmsg match {
    case Privmsg(nick, chan, cmd) =>
      val scmd = cmd.split(" ")
      if (scmd(0).equals("!active") || scmd(0).equals("!activity")) {
        val time = try {
          new StringOps(scmd(1)).toDouble
        } catch {
          case _:NumberFormatException => 6.0
          case _:ArrayIndexOutOfBoundsException => 6.0
        }
        println("Active: "+getActiveForTime(chan, time))
        sender ! "NOTICE "+nick+" :Users seen in the last "+time+" minutes: "+getActiveForTime(chan,time).mkString(" ")
      }

    case _ => {}
  }

  def receive = {
    case msg @ re(nick, chan) if chan(0) == '#' =>
      val time = System.nanoTime()
      
      // split the list to single out any old instance of that user's activity
      val (before, atAndAfter) = lastActive(chan) span {
        case (n, _) if n.toLowerCase.equals(nick.toLowerCase) => false
        case _ => true
      }
      val endOfList = if(atAndAfter.isEmpty) {
        atAndAfter
      } else {
        atAndAfter.tail
      }
      lastActive = lastActive + (chan->((before.+:(nick, time)) ++ endOfList))

      checkCmd(msg.toString)

    case Nick(oldnick, newnick) =>
      // any given nick should only appear once in the list,
      // so we need to stop looking through the list once
      // the first occurence has been found and changed
      lastActive = lastActive.mapValues(list => {
        val (before, atAndAfter) = list span {
          case (n, _) if n.toLowerCase.equals(oldnick.toLowerCase) => false
          case _ => true
        }

        // change the element that we split at to contain the new nick
        before ++ (atAndAfter.tail.+:(atAndAfter.head.copy(_1 = newnick)))
        // if this nick was never in the activity list, do nothing
        if (atAndAfter.isEmpty) {
          before
        } else {
          before ++ (atAndAfter.tail.+:(atAndAfter.head.copy(_1 = newnick)))
        }
      })

    case _ => {}
  }
}
