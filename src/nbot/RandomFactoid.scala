package nbot

import java.io.FileNotFoundException
import scala.io.Source
import scala.util.Random

object RandomFactoid extends CommandDelegate[String,String] {
  def safeStringToInt(str: String): Option[Int] = {
    import scala.util.control.Exception._
    catching(classOf[NumberFormatException]) opt str.toInt
  }

  // Maps a channel to a pairing of the last factoid used and the
  // map of factoids to their last number
  var lastFactoidUsed = ""
  var lastFactoidMap = Map[String,Int]()
  def tryCmd(key: String): Option[String] = {
    val splitkey = key.split(' ')
    val fname = splitkey(0)
    if (fname == "lastfactoid") {
      val r = splitkey.lift(1).getOrElse(lastFactoidUsed)
      lastFactoidMap.get(r).map(s"Last index for !$r was "+_)
    } else {
      try {
        val s = Source.fromFile("resources/randomfactoids/"+fname+".qt").getLines.toSeq
        // get the second argument of the command, or get a random number if it doesn't exist or is invalid
        val factoidNum = splitkey.lift(1).flatMap(safeStringToInt).getOrElse((new Random()).nextInt(s.length))

        lastFactoidMap = lastFactoidMap + ((fname,factoidNum))
        lastFactoidUsed = fname

        Some(s(factoidNum))
      } catch {
        case _:FileNotFoundException => None
      }
    }
  }
}
