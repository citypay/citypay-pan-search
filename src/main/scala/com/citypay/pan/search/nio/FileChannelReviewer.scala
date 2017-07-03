package com.citypay.pan.search.nio

import java.io.{File, RandomAccessFile}
import java.nio.channels.FileChannel

import com.citypay.pan.search._
import com.citypay.pan.search.source.{ScanListener, ScanSource}
import com.citypay.pan.search.util.Loggable

import scala.annotation.tailrec
import com.citypay.pan.search.util.Util.use


/**
  * Class which reviews a file channel by reading from the channel and collates numeric values of interest to an
  * inspection buffer. Should the inspection buffer meet review guidelines, the buffer will push to a review
  * process of configured specifications.
  *
  * Review guidelines are
  * - if a value is numeric, check the inspection buffer
  * - if the inspection buffer is a position 0, add the value only if the value matches any leading pan specs
  * - if the inspection buffer is a distance in position of 2 or more bytes then this is a false positive and reset
  * - if the inspection buffer is a position > 0 add the digit
  * - if the inspection buffer is available is 16 or more digits, review the data, if no match move the buffer forward
  * until the next matched spec leading digit
  *
  * @param file a file to be read from
  *
  */
class FileChannelReviewer(file: File,
                          val sec: ScanExecutionContext,
                          val source: ScanSource,
                          val system: String,
                          val location: String,
                          val scanListener: ScanListener
                         ) extends ScanExecutor with ScanReviewer with Loggable {


  @tailrec
  private def reviewFileChannel(channel: FileChannel, offset: Int, accumulativeResult: ReviewResult): ReviewResult = {

    // shortcut any matches if required
    if (ctx.stopOnFirstMatch && accumulativeResult.matches.nonEmpty) {
      accumulativeResult
    } else {

      // run an actual scan
      val read = channel.read(analysisBuffer, offset)
      if (read <= 0) {
        accumulativeResult // EOF file check, return the current matches found
      } else {

        val reviewed = reviewAnalysisBuffer(offset, read, location, accumulativeResult)

        //
        reviewFileChannel(
          channel,
          offset + read,
          accumulativeResult concat reviewed
        )


      }
    }
  }

  override def scan(): ScanReport = {
    use(new RandomAccessFile(file, "r")) { raf =>
      val result = reviewFileChannel(raf.getChannel, 0, ReviewResult())
      result.matches.foreach(m => {
        scanListener.matchFound(source, file.getName)
        report.addMatch(m)
      })
      report
    }
  }


  override def onComplete: Unit = {
  }


}

