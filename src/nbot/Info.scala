package nbot

import akka.actor.{Actor, ActorRef}
import scala.collection.immutable.Queue

import scala.slick.driver.SQLiteDriver.simple._
import Database.threadLocalSession

import scala.slick.session.Database
import scala.slick.jdbc.{GetResult, StaticQuery => Q}

/* The info file should be in resources/info.db
 * with the schema:
 * CREATE TABLE "info" (nick text, infotype text, info text, primary key(nick, infotype));
 */
class Info extends Actor {
  case class Info(nick:String, infotype:String, info:String)
  object Info extends Table[(String, String, String)]("info") {
    def nick = column[String]("nick", O.PrimaryKey)
    def infotype = column[String]("infotype", O.PrimaryKey)
    def info = column[String]("info")
    def * = nick ~ infotype ~ info
  }

  var allowedInfo = Set[String]()
  var nickCheckQueue = Queue[Info]()
  var use_status = false

  override def preStart = {
    context.actorSelection("../config").tell(SubscribeToKey("allowedinfo"), self)
    context.actorSelection("../config").tell(SubscribeToKey("use_status"), self)
  }

  def update_info(nick:String, expected:Info, digit:String) = {
    nickCheckQueue = nickCheckQueue.tail
    if (digit.equals("3") | digit.equals("2")) {
      Class.forName("org.sqlite.JDBC")

      Database.forURL("jdbc:sqlite:resources/info.db",driver="scala.slick.driver.SQLiteDriver") withSession {
        val nickToUse = if (expected.infotype.equals("alias")) {
          // if the user wants to chage their alias, it shouldn't redirect
          nick.toLowerCase
        } else {
          // modify the real entry if this user is updating from an alias
          // check if the user has an alias
          val aliascheck = for (a <- Info if a.nick === nick.toLowerCase && a.infotype === "alias") yield a.info
          // put the result of the alias check in an Option, and if it fails, use the entry for the given nick
          aliascheck.firstOption.getOrElse(nick).toLowerCase
        }

        if (expected.info.equals("")) {
          Query(Info).filter(i=>i.nick === nick.toLowerCase && i.infotype === expected.infotype).delete
          sender ! s"NOTICE $nick :Your ${expected.infotype} has been deleted successfully."
        } else {
          (Q.u + "REPLACE INTO info (nick, infotype, info) values ("
            +? nickToUse + "," +? expected.infotype + "," +? expected.info + ")").execute
          sender ! s"NOTICE $nick :Your ${expected.infotype} has been updated successfully."
        }
      }
    } else {
      sender ! s"NOTICE ${expected.nick} :You must be identified to do that!"
    }
  }

  def receive = {
    case KeyUpdate("allowedinfo", rawList:Seq[Any]) =>
      // only take strings, and convert to a set
      allowedInfo = (rawList collect {case s:String=>s}).toSet

    case KeyUpdate("use_status", us:Boolean) =>
        use_status = us

    case Privmsg(nick, _, msg) if msg.startsWith("!addfield ") =>
      msg.split(" ",3) match {
        case Array(_, category, rest @ _ *) =>
          if (allowedInfo(category)) {
            // An empty string marks the info field for deletion
            val newinfo = rest.headOption.getOrElse("")
            val nscommand = if (use_status) "STATUS" else "ACC"
            sender ! "PRIVMSG NickServ :"+nscommand+" "+nick
            nickCheckQueue = nickCheckQueue.enqueue(Info(nick, category, newinfo))
          }

        case _ => {}
      }

    case Notice("NickServ", _, msg) =>
      try {
        val expected = nickCheckQueue.front
        msg.split(" ",4).take(3) match {
          case Array(nick, "ACC", digit) if nick.toLowerCase.equals(expected.nick.toLowerCase) =>
              update_info(nick, expected, digit)

          case Array("STATUS", nick, digit) if nick.toLowerCase.equals(expected.nick.toLowerCase) =>
              update_info(nick, expected, digit)

          case _ => {}
        }
      } catch {
        case _:NoSuchElementException => {}
      }

    case Privmsg(nick, chan, msg) if msg(0) == '!' | msg(0) == '@' =>
      msg.substring(1).split(" ",3) match {
        case Array(category, targetNick, _@_*) =>
          val tNick = targetNick.toLowerCase

          Class.forName("org.sqlite.JDBC")

          Database.forURL("jdbc:sqlite:resources/info.db", driver = "scala.slick.driver.SQLiteDriver") withSession {
            // check if the user has an alias
            val aliascheck = for (a <- Info if a.nick === tNick && a.infotype === "alias") yield a.info
            // put the result of the alias check in an Option, and if it fails, use the entry for the given nick
            val nickToUse = aliascheck.firstOption.getOrElse(targetNick).toLowerCase
            val results = for(i <- Info if i.nick === nickToUse && i.infotype === category) yield i.info

            results foreach {
              sender ! (if(msg(0) == '@') s"PRIVMSG $chan :" else s"NOTICE $nick :") + s"$nickToUse's $category: "+_
            }
          }

        case _ => {}
      }

    case _ => {}
  }
}
