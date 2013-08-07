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

  override def preStart =
    context.actorSelection("../config").tell(SubscribeToKey("allowedinfo"), self)

  def receive = {
    case KeyUpdate("allowedinfo", rawList:Seq[Any]) =>
      // only take strings, and convert to a set
      allowedInfo = (rawList collect {case s:String=>s}).toSet

    case Privmsg(nick, _, msg) if msg.startsWith("!add ") =>
      msg.split(" ",3) match {
        case Array(_, category, newinfo) =>
          sender ! "PRIVMSG NickServ :ACC "+nick
          nickCheckQueue = nickCheckQueue.enqueue(Info(nick, category, newinfo))

        case _ => {}
      }

    case Notice("NickServ", _, msg) =>
      try {
        val expected = nickCheckQueue.front
        msg.split(" ",4).take(3) match {
          case Array(nick, "ACC", digit) if nick.toLowerCase.equals(expected.nick.toLowerCase) =>
            nickCheckQueue = nickCheckQueue.tail
            if (digit.equals("3") | digit.equals("2")) {
              Class.forName("org.sqlite.JDBC")

              Database.forURL("jdbc:sqlite:resources/info.db",driver="scala.slick.driver.SQLiteDriver") withSession {
                // modify the real entry if this user is updating from an alias
                // check if the user has an alias
                val aliascheck = for (a <- Info if a.nick === nick.toLowerCase && a.infotype === "alias") yield a.info
                // put the result of the alias check in an Option, and if it fails, use the entry for the given nick
                val nickToUse = aliascheck.firstOption.getOrElse(nick).toLowerCase

                (Q.u + "replace into info (nick, infotype, info) values ("
                  +? nickToUse + "," +? expected.infotype + "," +? expected.info + ")").execute
              }
              sender ! s"NOTICE ${expected.nick} :Your ${expected.infotype} has been updated successfully."
            } else {
              sender ! s"NOTICE ${expected.nick} :You must be identified to do that!"
            }

          case _ => {}
        }
      } catch {
        case _:NoSuchElementException => {}
      }

    case Privmsg(nick, chan, msg) if msg(0) == '!' | msg(0) == '@' =>
      val isPublic = msg(0) == '@'
      val smsg = msg.split(" ")
      val category = smsg(0).substring(1)
      val targetNick = smsg(1).toLowerCase

      Class.forName("org.sqlite.JDBC")

      Database.forURL("jdbc:sqlite:resources/info.db", driver = "scala.slick.driver.SQLiteDriver") withSession {
        // check if the user has an alias
        val aliascheck = for (a <- Info if a.nick === targetNick && a.infotype === "alias") yield a.info
        // put the result of the alias check in an Option, and if it fails, use the entry for the given nick
        val nickToUse = aliascheck.firstOption.getOrElse(targetNick).toLowerCase
        val results = for(i <- Info if i.nick === nickToUse && i.infotype === category) yield i.info

        results foreach {
          sender ! (if(isPublic) s"PRIVMSG $chan :" else s"NOTICE $nick :") + _
        }
      }

    case _ => {}
  }
}
