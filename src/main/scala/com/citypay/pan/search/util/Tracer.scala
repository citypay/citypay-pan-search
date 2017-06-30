package com.citypay.pan.search.util

object Tracer {

  val tracers: List[String] = Option(System.getProperty("pan.search.trace")).fold(List[String]())(s => s.split(",").toList)

  def contains(str: String): Boolean = tracers.exists(s => s.equalsIgnoreCase(str))

  val allEnabled: Boolean = contains("all")

}

/**
  * Tracer allows to write output to std out (or an alternative stream) and uses JVM keys to run, this allows
  * seperation from logging processes for very low level tracing.
  *
  * To enable a tracer process, implement a tracey key and set this tracekey value as a runtime property as -DtraceKey
  *
  */
class Tracer(_traceKey: String) {

  private var sb: StringBuffer = _

  lazy val isTracingEnabled: Boolean = Tracer.allEnabled || Tracer.contains(_traceKey)

  def print(): Unit = println(sb.toString)

  if (isTracingEnabled) {
    sb = new StringBuffer()
  }

  def trace(str: => String, args: Any*): Unit = {
    if (isTracingEnabled) sb.append(str.format(args: _*))
  }

  def tracer[T](str: String, args: Any*)(thunk: => T): T = {
    val t = thunk
    if (isTracingEnabled) {
      sb.append(str.format(args: _*))
      sb.append(" ...")
      sb.append(String.valueOf(t))
    }
    t
  }

  def traceEnd(): Unit = {
    if (isTracingEnabled) {
      print()
      sb.setLength(0)
    }
  }

  def traceEnd(str: String): Unit = {
    if (isTracingEnabled) {
      trace(str)
      traceEnd()
    }
  }


}
