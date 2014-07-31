package nbot

import scala.util.matching.Regex

trait IRCMessage {
  implicit class Regex(sc: StringContext) {
      def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
}

// plugins should never receive ping messages
// but I'll put this here anyways
case class Ping(response:String)
object Ping extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^PING :(.*)$response$$" =>
      Some(response)
    case _ => None
  }
}

case class Privmsg(nick:String, target:String, msg:String)
object Privmsg extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? PRIVMSG ([^ ]+)$target :(.*)$msg$$" =>
      Some(nick, target, msg)
    case _ => None
  }
}

case class Notice(nick:String, target:String, msg:String)
object Notice extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? NOTICE ([^ ]+)$target :(.*)$msg$$" =>
      Some(nick, target, msg)
    case _ => None
  }
}

case class Part(nick:String, target:String, msg:Option[String])
object Part extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? PART ([^ ]+?)$target( :.*)?$msg$$" =>
      Some(nick, target, Option(msg))
    case _ => None
  }
}

case class Join(nick:String, channel:String)
object Join extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:!.[^ ]+)? JOIN :(.+)$chan$$" =>
      Some(nick, chan)
    case _ => None
  }
}

case class Quit(nick:String, msg:String)
object Quit extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? QUIT :(.+)$msg$$" =>
      Some(nick, msg)
    case _ => None
  }
}

case class Kick(kicker:String, channel:String, kickee:String, msg:String)
object Kick extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? KICK ([^ ]+)$chan ([^ ]+)$kickee :(.*)$msg$$" =>
      Some(nick, chan, kickee, msg)
    case _ => None
  }
}

case class Nick(oldnick:String, newnick:String)
object Nick extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$oldnick(?:![^ ]+)? NICK :(.+)$newnick$$" =>
      Some(oldnick, newnick)
    case _ => None
  }
}

case class Names(channel:String, names:Seq[(Option[Char],String)])
object Names extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:[^ ]+ 353 [^ ]+ . ([^ ]+)$chan :(.*)$rawnames" =>
      val nl = for (n <- rawnames.split(' '):Seq[String]) yield 
        if (Set('+','~','@','&','%').contains(n(0))) (Some(n(0)),n.substring(1)) else (None,n)
      
      Some(chan, nl)
    case _ => None
  }
}

case class EndNames(channel:String)
object EndNames extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:.* 366 .+ (.+)$chan :.*" =>
      Some(chan)
    case _ => None
  }
}

case class Invite(nick:String, channel:String)
object Invite extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"^:([^! ]+)$nick(?:![^ ]+)? INVITE [^ ]+ :(.+)$chan$$" =>
      Some(nick, chan)
    case _ => None
  }
}

// SortedMode attempts to pair up mode chars with parameters.
// It is based off the list at https://www.alien.net.au/irc/chanmodes.html
// If a conflict exists, the one without parameters is used
// this currently does not work and is incomplete
/*
case class SortedMode(nick:String, channel:String,
  added:Seq[(Char,Option[String])], removed:Seq[(Char,Option[String])])
object SortedMode extends IRCMessage {
  def unapply(str: Any) = str match {
    case r"""^:([^! ]+)$nick(?:!.*)? MODE (.+)$chan (\+[^ ]+)?$add(-[^ ]+)?$sub (.*)$args$$""" =>
      println(add)
      println(sub)
      println(args)

      def modeFold (acc, elem:Char) = acc match {
        case (current:Seq[(Char,Option[String])], args:TraversableLike) =>
          elem match {
            case 'b'|'e'|'f'|'h'|'I'|'J'|'k'|'l'|'o'|'v'|'!' =>


      add.substring(1).toCharArray.foldLeft((Seq[(Char,Option[String])](), args.split(" "))) 

      Some(nick, chan, Seq(), Seq())
    case _ => None
  }
}
*/

case class Mode(nick:String, channel:String, added:IndexedSeq[Char], removed:IndexedSeq[Char], args:IndexedSeq[String])
object Mode extends IRCMessage {
  def unapply(str:Any) = str match {
    case r"""^:([^! ]+)$nick(?:![^ ]+)? MODE ([^ ]+)$chan (\+[^ ]+)?$add(-[^ ]+)?$sub(.*)$args$$""" =>
      val trimmed = args.trim
      Some(nick, chan,
        Option(add).map(_.substring(1).toIndexedSeq).getOrElse(IndexedSeq()),
        Option(sub).map(_.substring(1).toIndexedSeq).getOrElse(IndexedSeq()),
        if(trimmed.isEmpty) IndexedSeq() else trimmed.split(" ").toIndexedSeq)
    case _ => None
  }
}
//case class 
