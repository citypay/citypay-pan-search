package com.citypay.pan.search.util

import java.util.logging.LogManager

import org.slf4j.{Logger, LoggerFactory}

/**
  * Simple logging trait which wraps around SL4j
  * Created by gary on 09/06/2017.
  */
trait Loggable {

  def debug(str: => String): Unit = {
    if (logger.isDebugEnabled) logger.debug(str)
  }

  def debugr[T](str: => String)(thunk: => T): T = {
    val t = thunk
    if (logger.isDebugEnabled) {
      logger.debug(s"$str ...$t")
    }
    t
  }

  def log(str: => String): Unit = {
    if (logger.isInfoEnabled) logger.info(str)
  }

  def error(str: => String, t: Throwable): Unit = {
    if (logger.isErrorEnabled) logger.error(str, t)
  }

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

}
