package nbot

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress
import org.apache.commons.lang.StringEscapeUtils.escapeJava
import org.apache.commons.lang.StringUtils
import scala.collection.immutable.StringOps
import scala.util.matching.Regex
import scala.util.parsing.json.JSON
import scala.io.Source

class IRCClient(configFilename:String) extends Actor {
  import Tcp._
  import context.system

  // load the config file
  val config =
    JSON.parseFull(Source.fromFile(configFilename).mkString) match {
      case Some(c:Map[String,Any]) => c
      case _ => Map[String,Any]()
    }

  IO(Tcp) ! Connect(new InetSocketAddress(config("server").toString, config("port").asInstanceOf[Double].toInt))
  //IO(Tcp) ! Connect(remote)

  // The socket connection
  var connection:ActorRef = null

  val nick:String = config.getOrElse("nick","NBot").toString
  val user:String = config.getOrElse("user",nick).toString

  var plugins = List(
     context.actorOf(Props(classOf[ConfigFile], config, configFilename), name = "config")
     //context.actorOf(Props(new ConfigFile(config, configFilename)), name = "config")
    //,context.actorOf(Props(new Channeljoin(nick)), name = "channeljoin") ,context.actorOf(Props[Factoids], name = "factoids")
    ,context.actorOf(Props(classOf[Channeljoin], nick), name = "channeljoin")
    ,context.actorOf(Props[Factoids], name = "factoids")
    ,context.actorOf(Props[Nicklist], name= "nicklist")
    ,context.actorOf(Props[Activity], name= "activity")
    ,context.actorOf(Props[Mfk], name = "mfk")
    ,context.actorOf(Props[Callops], name = "callops")
    ,context.actorOf(Props[Tell], name = "tell")
  )

  // for some reason I have to send the USER message
  // after the first ping
  var pings = false

  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self

    case c @ Connected(remote, local) =>
      //listener ! c
      connection = sender
      connection ! Register(self)
      self ! "NICK "+nick
      self ! "USER "+user+" 0 * : "+user
      //self ! "USER "+user+" 0 * : "+user

    case data:String =>
      println("-> "+data)
      connection ! Write(ByteString(data+"\r\n"))

    case Received(data) =>
      val dataStr = data.decodeString("US-ASCII") //.replaceAll("""(?m)\s+$""", "")
      println("<- "+escapeJava(dataStr))

      // split in case we get more than one line in the same message
      dataStr.split("\n").map(_.stripSuffix("\r")) foreach {
        case Ping(response) =>
          self ! "PONG :"+response
          if (pings == false) {
            pings = true
            self ! "USER "+user+" 0 * : "+user
          }
        case msg =>
          //plugins.foreach((p:ActorRef) => p ! msg)
          context.children.foreach((p:ActorRef) => p ! msg)
      }
         case _: ConnectionClosed =>
      context stop self
  }
}
