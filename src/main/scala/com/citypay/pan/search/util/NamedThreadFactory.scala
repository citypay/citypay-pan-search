package com.citypay.pan.search.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by gary on 08/06/2017.
  */
case class NamedThreadFactory(name: String, daemon: Boolean = true) extends ThreadFactory {

  val id = new AtomicInteger(0)

  def newThread(r: Runnable): Thread = {
    val t = new Thread(r, "%s-%s".format(name, this.id.getAndIncrement))
    t.setDaemon(daemon)
    t
  }
}
