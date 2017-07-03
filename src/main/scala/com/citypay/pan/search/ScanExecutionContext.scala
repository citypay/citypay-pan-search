package com.citypay.pan.search

import java.util.concurrent._

import com.citypay.pan.search.source.{ScanListener, ScanSource}
import com.citypay.pan.search.util.{Lockable, Loggable, NamedThreadFactory}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Defines the context of a scan during its execution stage
  *
  * @param sc     the context of what is being searched
  * @param name   the name of the execution
  * @param report the report which is collating information on the execution
  */
class ScanExecutionContext(val sc: SearchContext,
                           val name: String,
                           val report: ScanReport) extends Lockable with Loggable {


  private val started = System.currentTimeMillis()
  private val queue = new ArrayBlockingQueue[Runnable](16 * 1024) {
    override def offer(e: Runnable): Boolean = {
      try {
        put(e)
        true
      } catch {
        case ie: InterruptedException => Thread.currentThread().interrupt()
      }
      false
    }
  }

  private val phaser = new Phaser(1)
  //  private val rejectedExecutionHandler = new RejectedExecutionHandler {
  //    override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
  //      log("Rejected")
  //      report.incStat(ScanReport.StandardStats.Error)
  //    }
  //  }

  private val executor: ThreadPoolExecutor = new ThreadPoolExecutor(
    sc.config.scanWorkers, sc.config.scanWorkers, 0, TimeUnit.SECONDS, queue,
    NamedThreadFactory(s"scanner-${name.filter(_.isLetter)}"),
    new ThreadPoolExecutor.CallerRunsPolicy())

  implicit private val ec = ExecutionContext.fromExecutor(executor)


  def await(): ScanReport = lock {
    debug(s"scan items queued (${phaser.getRegisteredParties - 1}), awaiting on executor $name")
    phaser.arriveAndAwaitAdvance()
    report.markComplete(System.currentTimeMillis() - started)
    debug(s"executor $name complete")
    report
  }

  def queue(source: ScanSource, listener: ScanListener, ex: ScanExecutor): Option[Future[ScanReport]] = lock {

    if (executor.isShutdown || executor.isTerminating || executor.isTerminated) {
      None
    } else {


      // set to Java concurrency implementation due to scala's Promise/Future process async submission process
      // we immediately obtain a RejectedExecutionException if we are unable to submit as this point

      phaser.register()
//      debug(s"Registered $phaser")

      listener.anticipated(source, 1)

      Some {
        executor.submit(() => {
          try {
//            debug("Starting scan")
            ex.scan()
          } catch {
            case NonFatal(e) =>
              error(s"Failed to scan $name", e)
              throw e
          } finally {
            ex.onComplete
            phaser.arriveAndDeregister()
//            debug(s"Deregister $phaser")
          }
        })
      }


    }


  }

  /**
    * Cancels the execution process, immediately shutting down the execution process. An attempt is made to interrupt
    * the executor and will wait for the given timeout
    *
    */
  def cancel(timeout: Long, unit: TimeUnit): Unit = lock {
    executor.shutdownNow()
    executor.awaitTermination(timeout, unit)
  }

  /**
    * Called to mark the end of execution by sending in a "poison" task which shuts down the executor and notifies
    * any threads waiting
    */
  def end(): Unit = lock {
    executor.submit(new Runnable {
      override def run(): Unit = {
        executor.shutdown()
        debug("Shutdown execution context")
      }
    })
  }

  def taskSize: Int = queue.size()


}