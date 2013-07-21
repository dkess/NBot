package nbot

import akka.actor.{Actor, ActorRef}
import akka.agent.Agent
import akka.actor.Status.Failure

case class NicklistMeta(nicklist:Map[String,Set[(Option[Char], String)]], channel:String, cmd: String)
case class FetchResults(channel:String, cmd:String)

class Nicklist extends Actor with CommandDelegator[NicklistMeta,Set[String]] {
  //implicit val system = context.system
  var nicklist = Map() : Map[String,Set[(Option[Char], String)]]

  // NAMES messages don't come all at once, so we have to know if the NAMES message we get
  // is a continuation of a previous one
  var nicklistsInProgress = Set(): Set[String]
  
  val delegates = Seq(NickGroupFile, GetOps, new NickRPN(runCmd))
  
  def removeNick(nick:String)(list:Set[(Option[Char], String)]):Set[(Option[Char], String)] =
    list.filterNot({
      case (_, s) if s.equals(nick) => true
      case _ => false
    })

  def receive = {
    case Privmsg(nick, target, msg) if msg(0) == '!' =>
      runCmd(NicklistMeta(nicklist, target, msg.substring(1))) foreach {
        sender ! s"NOTICE $nick :"+_.mkString(" ")
      }
    // for debug purposes
    case Privmsg(_, _, msg) if msg.equals("names") =>
      println(nicklist)

    case FetchResults(chan, cmd) =>
      println(NicklistMeta(nicklist, chan, cmd))
      val r = runCmd(NicklistMeta(nicklist, chan, cmd)).getOrElse(new Failure(new Throwable()))
      sender ! r

    case Names (chan, list) =>
      val channelNames = if (nicklistsInProgress.contains(chan)) {
        nicklist(chan) ++ Set(list: _*)
      } else {
        Set(list: _*)
      }
      nicklist = nicklist + (chan -> channelNames)
      nicklistsInProgress = nicklistsInProgress + chan
    case EndNames(chan) =>
      nicklistsInProgress = nicklistsInProgress - chan
    case Part (nick, chan, _) =>
     nicklist = nicklist + (chan -> removeNick(nick)(nicklist(chan)))
    case Join(nick, chan) =>
      if(nicklist.contains(chan))
        nicklist = nicklist + (chan -> (nicklist(chan).+((None,nick))))
    case Quit(nick, _) =>
      nicklist = nicklist.mapValues(removeNick(nick))
    case Kick(_, chan, nick, _) =>
      nicklist = nicklist + (chan -> removeNick(nick)(nicklist(chan)))
    case Nick(oldnick, newnick) =>
      nicklist = nicklist.mapValues(_ map {
        case (r, n) if n.equals(oldnick) => (r,newnick)
        case other => other
      })
    case Mode(_, chan, added, removed, _)
    if added.exists("hoqOva".contains(_)) | removed.exists("hoqOva".contains(_)) =>
      // get a new NAMES list so we know what the updated symbols are
      sender ! "NAMES "+chan
    case _ => None
  }
}
