package nbot

trait CommandDelegate[A,B] {
  def tryCmd(cmd: A): Option[B]
}
