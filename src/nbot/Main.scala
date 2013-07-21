package nbot

import akka.actor.{Actor, ActorSystem}
import akka.actor.Props
import java.net.InetSocketAddress

object Main {
  def main(args: Array[String]) {
    val system = ActorSystem("Actors")
    val clientActor = system.actorOf(Props(new IRCClient("config.json")))

  }
}
