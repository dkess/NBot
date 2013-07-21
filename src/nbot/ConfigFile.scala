package nbot

import akka.actor.{Actor, ActorRef}
import akka.actor.Status.Failure
import scala.util.parsing.json.JSON
import scala.io.Source

case class GetFullConfig()
case class GetConfigKey(key:String)

case class SubscribeToKey(key:String)
case class UnsubstribeFromKey(key:String)
case class KeyUpdate(key:String, value:Any)

class ConfigFile(initialConfig:Map[String,Any], filename:String) extends Actor {
  var config:Map[String,Any] = initialConfig
  val configFileName = filename
  
  //override def preStart() = reload
  
  // mapping from a key to a set of its subscribers
  var keySubscriptions = Map[String, Set[ActorRef]]().withDefaultValue(Set[ActorRef]())

  def reload =
    JSON.parseFull(Source.fromFile(configFileName).mkString) match {
      case Some(newconfig:Map[String,Any]) =>
        // find values that have changed, and send a new message to
        // those subscribers
        newconfig foreach {
          case (k:String, v:Any) if !v.equals(config.get(k).orNull) =>
            keySubscriptions(k) foreach {
              _ ! KeyUpdate(k, v)
            }
          case _ => {}
        }
        config = newconfig
      case _ => {}
    }

  def receive = {
    // for debug purposes
    case Privmsg(_,_,"printconfig") =>
      println(config)
    case Privmsg(_,_,"reload-config") =>
      reload

    case SubscribeToKey(key) =>
      keySubscriptions = keySubscriptions + (key->(keySubscriptions(key) + sender))
      config.get(key).foreach(v => sender ! KeyUpdate(key, v))

    case GetFullConfig() =>
      sender ! config
    case GetConfigKey(key) =>
      sender ! config.getOrElse(key,new Failure(new Throwable()))

    case _ => {}
  }
}
