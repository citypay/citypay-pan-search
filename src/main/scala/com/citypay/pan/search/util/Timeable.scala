package com.citypay.pan.search.util

trait Timeable extends Loggable {

  /**
    * Creates a timed piece of code eg
    * Timed("block of code") {
    *    ///
    *  }
    * @param desc a description to be logged on the code block
    * @param count a count which can be used to average out the time taken
    *              when timing a looped block of code, if this value
    *              is greater than 1 (default) then an average is calculated
    */
  case class Timed(desc: String, count: Int = 1) {

    def calc[T](fn: => T, log: Double => {}): T = {
      val start = System.nanoTime()
      try {
        fn
      } finally {
        val taken = (System.nanoTime() - start) / 1000000d
        log(taken)
      }
    }

    def apply[T](fn: => T): T = {

      def comment(taken: Double):String = {

        if (count == 1) {
          f"...$desc%15s took $taken%03.3f ms"
        }
        else {
          val avg = taken / count
          f"...$desc%15s took $taken%03.2fms, avg($count): $avg%03.2fms"
        }

      }

      if (logger.isDebugEnabled()) {
        calc(fn, d => {
          debug(comment(d))
          0
        })
      } else {
        fn
      }

    }
  }

}
