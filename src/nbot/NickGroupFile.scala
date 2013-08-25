package nbot

import java.io.FileNotFoundException
import scala.io.Source

object NickGroupFile extends CommandDelegate[NicklistMeta,Set[String]] {
  def tryCmd(key: NicklistMeta): Option[Set[String]] =
    try {
      val file = Source.fromFile("resources/nicklists/"+key.cmd.split(' ')(0) + ".ls")
      // compile the regexes beforehand because they will be used
      // multiple times
      val nickFileRegexes = file.getLines.map(_.r).toSeq
      println("regexes:\n"+nickFileRegexes)
      println("nicklist:\n"+key.nicklist(key.channel).map(_._2))
      Some(key.nicklist(key.channel).map(_._2).filter((n:String)=>
          nickFileRegexes.exists(_.findFirstIn(n.toLowerCase).nonEmpty)))

    } catch {
      case _:FileNotFoundException => None
    }
}
