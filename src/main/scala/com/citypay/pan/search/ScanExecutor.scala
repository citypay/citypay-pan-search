package com.citypay.pan.search

/**
  * SImple trait which runs a scan and returns a report
  */
trait ScanExecutor {

  def scan(): ScanReport

  def onComplete: Unit

}


class NoOpScan(sec: ScanExecutionContext) extends ScanExecutor {
  override def scan(): ScanReport = sec.report
  override def onComplete: Unit = {}
}