package nbot

import java.io.FileNotFoundException
import scala.io.Source

object NickGroupFile extends CommandDelegate[NicklistMeta,Set[String]] {
  def nicksFromSet(ircNicks:Set[String], listToCompareWith:Set[String]): Set[String] =
    ircNicks.filter((n:String) => 
      listToCompareWith.exists((d:String) => 
        n.toLowerCase.contains(d.toLowerCase)))

  def tryCmd(key: NicklistMeta): Option[Set[String]] =
    try {
      val file = Source.fromFile("resources/nicklists/"+key.cmd.split(' ')(0) + ".ls")
      // get rid of the tag (first element of nick tuple)
      // TODO: you can get elements of a tuple with _1 and _2 etc.
      // implement that since I didn't know that could be done
      // at the time this was written
      Some(nicksFromSet(key.nicklist(key.channel) map { case (_,b) => b },
        file.getLines.toSet))
    } catch {
      case _:FileNotFoundException => None
    }
}
