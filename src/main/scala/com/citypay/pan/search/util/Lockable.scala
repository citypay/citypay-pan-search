package com.citypay.pan.search.util

import java.util.concurrent.locks.{Condition, ReentrantLock}

trait Lockable {

  protected val lock: ReentrantLock = new ReentrantLock(true)
  protected val cond1: Condition = lock.newCondition()

  def lock[X](func: => X): X = {
    lock.lock()
    try {
      func
    } finally {
      lock.unlock()
    }
  }

}
