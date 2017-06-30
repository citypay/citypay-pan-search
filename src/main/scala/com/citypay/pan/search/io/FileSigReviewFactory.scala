package com.citypay.pan.search.io

import java.io.{File, RandomAccessFile}

import com.citypay.pan.search._
import com.citypay.pan.search.nio.FileChannelReviewer
import com.citypay.pan.search.source.{ScanListener, ScanSource}
import com.citypay.pan.search.util.{Loggable, Util}
import net.sf.sevenzipjbinding.ArchiveFormat


/**
  * Factory which analyses a file and generates a reviewer based on the signature of a file
  */
class FileSigReviewFactory(file: File,
                           val sec: ScanExecutionContext,
                           val source: ScanSource,
                           val system: String,
                           val location: String,
                           val scanListener: ScanListener
                          ) extends ScanExecutor with ScanReviewer with Loggable {

  override def scan(): ScanReport = {

    // inspect file for signature patterns
    val prefix = new Array[Byte](FileSignatures.ByteLen)
    Util.use(new RandomAccessFile(file, "r")) {
      _.read(prefix)
    }

    // attempt to identify file signature and delegate based on findings
    val delegate = FileSignatures.List.find(fs => fs._1.matches(prefix)).fold[ScanExecutor]({
      sec.report.incStat("FileScan")
      new FileChannelReviewer(file, sec, source, system, file.toString, scanListener)
    })(sig => {
      debug(s"Found ${sig._1.name} signature on file ${file.getName} -> ${sig._2}")
      sig._2 match {
        case a: ArchiveFormat if sec.sc.config.scanArchives =>
          sec.report.incStat("ArchiveScan")
          new ArchiveScanner(a, file, sec, source, system, file.toString, scanListener)
        case a: ArchiveFormat =>
          sec.report.incStat("NoOp")
          new NoOpScan(sec)
        case None =>
          sec.report.incStat("NoOp")
          new NoOpScan(sec)
        case _ =>
          sec.report.incStat("FileScan")
          new FileChannelReviewer(file, sec, source, system, file.toString, scanListener)
      }
    })


    try {
      delegate.scan()
    } finally {
      delegate.onComplete
    }

  }

  override def onComplete: Unit = {
  }

}
