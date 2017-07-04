package com.citypay.pan.search.source

import java.time.LocalDateTime

import com.citypay.pan.search.source.CommandLineScanListener.started
import com.citypay.pan.search.{ScanExecutionContext, ScanExecutor, ScanReport, SearchContext}

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

/**
  * A source to be scanned by the scanning process such as a database, filesystem.
  * Contains a scan function which executes the scan
  */
abstract class ScanSource {

  protected var _listener: ScanListener = NoOpScanListener

  def attachListener(listener: ScanListener): Unit = {
    if (listener != null)
      _listener = listener
  }


  def name: String

  /**
    * Runs the underlying scan against the scan source
    * @return scan reports which are divided by the underlying source, for instance a file system scan may produce
    *         a report based on each filesystem or a database based on each schema
    */
  def submitScan(context: SearchContext): List[ScanReport]

  /**
    * Close scan source
    */
  def close(): Unit

  def queue(ctx: ScanExecutionContext, ex: ScanExecutor): Unit = try {
    ctx.queue(this, _listener, ex)
  } catch {
    case NonFatal(e) => ctx.report.incStat(ScanReport.StandardStats.Error)
  }


}


trait ScanListener {
  def complete(scanSource: ScanSource): Unit
  def started(scanSource: ScanSource): Unit
  def anticipated(scanSource: ScanSource, count: Int): Unit
  def scanItem(scanSource: ScanSource, name: String): Unit
  def matchFound(scanSource: ScanSource, desc: String): Unit
}

object NoOpScanListener extends ScanListener {
  override def complete(scanSource: ScanSource): Unit = {}
  override def started(scanSource: ScanSource): Unit = {}
  override def anticipated(scanSource: ScanSource, count: Int): Unit = {}
  override def scanItem(scanSource: ScanSource, name: String): Unit = {}
  override def matchFound(scanSource: ScanSource, desc: String): Unit = {}
}

object CommandLineScanListener extends ScanListener {

  class ThreadScanListener(val src: ScanSource) extends ScanListener {

    val thread: Thread = Thread.currentThread()
    var count: Int = 0
    var anticipated: Int = 0
    var matchCount: Int = 0
    private var started = LocalDateTime.now()

    def percentageComplete = {
      if (count == 0 || anticipated == 0)
        0.0
      else
        100.0 * (count.toDouble / anticipated.toDouble)
    }

    override def complete(scanSource: ScanSource): Unit = {
      started = LocalDateTime.now()
    }
    override def started(scanSource: ScanSource): Unit = {
      started = LocalDateTime.now()
    }

    override def scanItem(scanSource: ScanSource, name: String): Unit = {
      count = count + 1
      if (count % 100 == 0) {
        printData()
      }
    }

    override def matchFound(scanSource: ScanSource, desc: String): Unit = {
      matchCount = matchCount + 1
      printData()
    }

    override def anticipated(scanSource: ScanSource, count: Int): Unit = {
      anticipated = anticipated + count
    }

    def duration: String = {
      import java.time.temporal.ChronoUnit
      val secs = ChronoUnit.SECONDS.between(started, LocalDateTime.now()) % 60
      val minutes = ChronoUnit.MINUTES.between(started, LocalDateTime.now()) % 60
      val hours = ChronoUnit.HOURS.between(started, LocalDateTime.now())
      "%02d:%02d:%02d".format(hours, minutes, secs)
    }

  }

  private var threads = ListBuffer[ThreadScanListener]()

  def printData(): Unit = {

    val sb = new StringBuilder

    threads.foreach(t => {
      sb.append("\r")
      sb.append("%20s".format(t.thread.getName))
      sb.append("|")
      val i = t.percentageComplete
      val s = "=" * (i.toInt / 4)
      sb.append(s)
      sb.append(">")
      sb.append(" " * (25 - s.length))
      sb.append(" | A: %08d".format(t.anticipated))
      sb.append(" | C: %08d".format(t.count))
      sb.append(" | M: %08d".format(t.matchCount))
      sb.append(" | %03d%%".format(i.toInt))
      sb.append(" | %s".format(t.duration))
//      sb.append("\n")
    })

    print(sb.toString)

  }

  override def complete(scanSource: ScanSource): Unit = {
    for ((t, i) <- threads.zipWithIndex) {
      if (t.src == scanSource) {
        threads.remove(i)
      }
    }
  }

  override def started(scanSource: ScanSource): Unit = {
    threads += new ThreadScanListener(scanSource)
  }

  override def scanItem(scanSource: ScanSource, name: String): Unit = {
    threads.find(_.src == scanSource).foreach(_.scanItem(scanSource, name))
  }

  override def matchFound(scanSource: ScanSource, desc: String): Unit = {
    threads.find(_.src == scanSource).foreach(_.matchFound(scanSource, desc))
  }

  override def anticipated(scanSource: ScanSource, count: Int): Unit = {
    threads.find(_.src == scanSource).foreach(_.anticipated(scanSource, count))
  }
}