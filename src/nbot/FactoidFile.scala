package nbot

import scala.io.Source

object FactoidFile extends CommandDelegate[String,String] {
  def getKeyIter(key: String, it: Iterator[String]) : Option[String] = {
    if (it.hasNext) {
      // split the line into words and test if any of them are equal to key
      if ((it.next.toLowerCase.split(" ") : Seq[String]).exists(k => k.equals(key.toLowerCase))) {
        // If we find a match, return the next line
        Some(it.next)
      } else {
        // Otherwise, get the next line, discard it, and recurse
        it.next()
        getKeyIter(key, it)
      }
    } else {
      None
    }
  }

  def tryCmd(key: String): Option[String] =
    // We only want the first word
    getKeyIter(key.split(' ')(0), Source.fromFile("resources/factoids.txt").getLines)
}
