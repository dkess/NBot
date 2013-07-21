package nbot

trait CommandDelegator[A,B] {
  val delegates:Iterable[CommandDelegate[A,B]]
  
  private def runCmdIter(cmd: A, i:Iterator[CommandDelegate[A,B]]): Option[B] =
    if (i.hasNext) {
      i.next.tryCmd(cmd).orElse(runCmdIter(cmd, i))
    } else {
      None
    }

  def runCmd(cmd: A): Option[B] =
    runCmdIter(cmd, delegates.iterator)
}
