package com.citypay.pan.search

import com.typesafe.config.Config


object ScannerConfig {

  def apply(cfg: Config): ScannerConfig = new ScannerConfig {

    override def scanWorkers: Int = cfg.getInt("scanWorkers")

    override def concurrentScans: Int = cfg.getInt("concurrentScans")

    override def analysisBufferSz: Int = cfg.getInt("analysisBufferSz")

    override def scanArchives: Boolean = cfg.getBoolean("scanArchives")

    override def stopOnFirstMatch: Boolean = cfg.getBoolean("stopOnFirstMatch")
  }

}

trait ScannerConfig {

  /**
    * @return the number of concurrent scans to perform based on source configuration.
    */
  def concurrentScans: Int

  /**
    * @return the number of works used for a scanning process, the total number of scan works may be [[concurrentScans]]
    *         * [[scanWorkers]]
    */
  def scanWorkers: Int

  /**
    * whether to scan archive files, currently the SevenZipJBinding has known crashes due to ulimit settings and is not currently recommended
    * @return
    */
  def scanArchives: Boolean

  /**
    * @return the size of the buffer to use when reading from the file, defaults to 16384
    */
  def analysisBufferSz: Int = 1024 * 16

  /**
    * Determines if a scan will stop on the first match of a scan for instance if searching a file and a value is found
    * there is no need to scan further in the file to see if card holder data exists,
    * @return
    */
  def stopOnFirstMatch: Boolean


}
