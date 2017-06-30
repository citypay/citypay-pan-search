package com.citypay.pan.search

/**
  * Created by gary on 12/06/2017.
  */
class UnitTestScannerConfig extends ScannerConfig {
  override def concurrentScans: Int = 1
  override def scanWorkers: Int = 1
  override def scanArchives: Boolean = true
  override def stopOnFirstMatch: Boolean = true
}
