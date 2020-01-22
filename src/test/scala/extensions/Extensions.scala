package extensions

trait Extensions {

  implicit class RepeatExtension(n: Int) {
    def times[A](block: Int => A): Unit = (1 to n).foreach(block)
  }

}
