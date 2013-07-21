package nbot

import akka.actor.{Actor, ActorRef}

class Channeljoin(nick:String) extends Actor {
  var currentlyIn = Set[String]()
  var channelsFromConfig = Set[String]()
  var kickedFrom = Set[String]()
  var removedFromConfig = Set[String]()

  var autorejoin = false

  var botnick = nick
  
  var clientActor:Option[ActorRef] = None

  override def preStart = {
    val config = context.actorSelection("../config")
    config.tell(SubscribeToKey("channels"), self)
    config.tell(SubscribeToKey("autorejoin"), self)
  }

  def receive = {
    // for debug purposes
    case Privmsg(_,_,"channellists") =>
      println("currently in "+currentlyIn)
      println("from config  "+channelsFromConfig)
      println("kicked from  "+kickedFrom)

    case Invite(_, chan) =>
      sender ! "JOIN "+chan
    case msg:String if msg.matches("^:.* 001 .*") =>
      clientActor = Some(sender)
      sender ! "JOIN "+channelsFromConfig.mkString(",")

    case Join(nick, chan) if nick.equals(botnick) =>
      currentlyIn = currentlyIn + chan
      kickedFrom = kickedFrom - chan
    case Kick(_, chan, nick, _) if nick.equals(botnick) =>
      currentlyIn = currentlyIn - chan
      if (autorejoin) {
        sender ! "JOIN "+chan
      }
      kickedFrom = kickedFrom + chan
    case Part(nick, chan, _) if nick.equals(botnick) =>
      currentlyIn = currentlyIn - chan
      if (removedFromConfig(chan)) {
        removedFromConfig = removedFromConfig - chan
      } else {
        if (autorejoin) {
          sender ! "JOIN "+chan
        }
        kickedFrom = kickedFrom + chan
      }

    case Nick(nick, newnick) if nick.equals(botnick) =>
      botnick = newnick

    case KeyUpdate("channels", rawChannels:Seq[Any]) =>
      // only take elements that are strings
      val channels:Seq[String] = rawChannels collect {case s:String => s}
      println("got channels update "+channels)
      val chanSet = channels.toSet
      clientActor foreach (client => {
        // leave any channels that we joined from the old config
        val toLeave = channelsFromConfig.diff(chanSet).intersect(currentlyIn)
        removedFromConfig = removedFromConfig ++ toLeave
        client ! "PART "+toLeave.mkString(",")
        // join new channels, unless we've been kicked from them
        client ! "JOIN "+chanSet.diff(currentlyIn).diff(kickedFrom).mkString(",")
      })
      channelsFromConfig = chanSet

    case KeyUpdate("autorejoin", newval:Boolean) =>
      autorejoin = newval

    case _ => {}
  }
}
