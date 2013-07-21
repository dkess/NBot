package nbot

import java.io.FileNotFoundException
import scala.io.Source
import scala.util.Random

object RandomFactoid extends CommandDelegate[String,String] {
  def tryCmd(key: String): Option[String] =
    try {
      val s = Source.fromFile("resources/randomfactoids/"+key.split(' ')(0)+".qt").getLines.toSeq
      Some(s((new Random()).nextInt(s.length)))
    } catch {
      case _:FileNotFoundException => None
    }
}
