package com.citypay.pan.search

import com.citypay.pan.search.source.ScanSource

/**
  * Created by gary on 12/06/2017.
  */
class UnitTestSearchContext extends SearchContext {
  override def level1: List[PanSpec] = Nil
  override def sources: List[ScanSource] = Nil
  override def stopOnFirstMatch: Boolean = true
  override def config: ScannerConfig = new UnitTestScannerConfig
}
