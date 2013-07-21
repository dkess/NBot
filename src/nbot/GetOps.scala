package nbot

object GetOps extends CommandDelegate[NicklistMeta,Set[String]] {
  def tryCmd(key: NicklistMeta): Option[Set[String]] = key match {
    case NicklistMeta(nicklist, chan, "ops") =>
      Some((for ((rank,name) <- nicklist(chan) if rank.map("~@&%".contains(_)).getOrElse(false)) yield name).toSet)
    case _ => None
  }
}
