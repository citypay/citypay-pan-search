// MIT License
//
// Copyright (c) 2017 CityPay
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.citypay.pan.search.io

import java.io.{File, FileOutputStream, RandomAccessFile}

import com.citypay.pan.search._
import com.citypay.pan.search.nio.FileChannelReviewer
import com.citypay.pan.search.source.{ScanListener, ScanSource}
import com.citypay.pan.search.util.Util.use
import com.citypay.pan.search.util.{Loggable, Timeable}
import net.sf.sevenzipjbinding._
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object ArchiveScanner {

  val ArchiveFormats: Map[String, ArchiveFormat] = Map(
    ".zip" -> ArchiveFormat.ZIP,
    ".gz" -> ArchiveFormat.GZIP,
    ".gz" -> ArchiveFormat.BZIP2,
    ".7zip" -> ArchiveFormat.SEVEN_ZIP,
    ".7z" -> ArchiveFormat.SEVEN_ZIP
  )

  def isArchive(file: File): Boolean = false
}

protected abstract class Extract extends IArchiveExtractCallback with ScanReviewer {

  def archive: IInArchive

  override def setOperationResult(extractOperationResult: ExtractOperationResult): Unit = {}

  override def prepareOperation(extractAskMode: ExtractAskMode): Unit = {}

  override def setTotal(total: Long): Unit = {}

  override def setCompleted(complete: Long): Unit = {}

  override def getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream = {

    val skipExtraction = archive.getProperty(index, PropID.IS_FOLDER).asInstanceOf[Boolean]
    if (skipExtraction || extractAskMode != ExtractAskMode.EXTRACT) {
      //noinspection ScalaStyle
      return null
    }
    val _location = String.valueOf(archive.getProperty(index, PropID.NAME))

    new ISequentialOutStream {
      override def write(data: Array[Byte]): Int = inspect(_location, data)
    }
  }

  def inspect(location: String, data: Array[Byte]): Int

  def extract(ints: Array[Int]): ScanReport = {
    try {
      archive.extract(ints, false, this)
      report
    } finally {
      complete()
    }
  }

  def complete(): Unit = {}

}

protected[io] class TmpScanner(val archive: IInArchive,
                               val sec: ScanExecutionContext,
                               val source: ScanSource,
                               val location: String,
                               val system: String,
                               val scanListener: ScanListener
                              ) extends Extract {

  private var file: Option[File] = None
  private var fos: Option[FileOutputStream] = None
  private var idx = -1
  private val stack = ListBuffer[(String, File)]()

  override def getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream = {

    if (idx != index) {
      // index change so either first file or next file

      closeOutputStreams()
      idx = index
      val _file = File.createTempFile(".archive-scanner", ".tmp")
      val _location = String.valueOf(archive.getProperty(index, PropID.NAME))
      stack += ((_location, _file))
      file = Some(_file)
      fos = Some(new FileOutputStream(_file))
    }

    super.getStream(index, extractAskMode)
  }

  override def inspect(_location: String, data: Array[Byte]): Int = {
    // simply write to tmp location
    fos.foreach(_.write(data))
    data.length
  }

  private def closeOutputStreams(): Unit = {
    fos.foreach(_.close())
  }

  override def complete(): Unit = {

    closeOutputStreams()

    // now run scans against all files...
    stack.foreach(d => {
      val file = d._2
      val loc = d._1
      try {
        report.add(new FileChannelReviewer(file, sec, source, system, loc, scanListener).scan())
      } finally {
        file.delete()
      }
    })
  }


}


protected[io] class VRamScanner(val archive: IInArchive,
                                val sec: ScanExecutionContext,
                                val location: String,
                                val system: String,
                                val scanListener: ScanListener
                               ) extends Extract {

  override def inspect(_location: String, data: Array[Byte]): Int = {

    val cap = analysisBuffer.capacity()

    val result = if (data.length < cap) {

      analysisBuffer.rewind()
      analysisBuffer.put(data, 0, data.length)
      reviewAnalysisBuffer(0, data.length, location, ReviewResult())

    } else {

      @tailrec
      def fn(offset: Int, remaining: Int, accumulativeResult: ReviewResult): ReviewResult = {
        val read = if (remaining > cap) cap else remaining

        analysisBuffer.rewind()
        analysisBuffer.put(data, offset, read)
        val reviewed = reviewAnalysisBuffer(offset, data.length, location, accumulativeResult)

        fn(offset + read, remaining - read, accumulativeResult concat reviewed)
      }

      fn(0, data.length, ReviewResult())

    }

    result.matches.map(_.copy(location = _location)).foreach(report.addMatch)
    data.length

  }

}


/**
  * Created by gary on 14/06/2017.
  */
class ArchiveScanner(archiveFormat: ArchiveFormat,
                     file: File,
                     val sec: ScanExecutionContext,
                     val source: ScanSource,
                     val system: String,
                     val location: String,
                     val scanListener: ScanListener
                    ) extends ScanExecutor with ScanReviewer with Timeable with Loggable {

  // limit imposed that an archive can be expanded in runtime memory otherwise it will be extracted to a tmp file
  private val inMemoryLimit = Runtime.getRuntime.maxMemory() / 16

  override def scan(): ScanReport = {


    use(new RandomAccessFile(file, "r")) { raf =>
      use(new RandomAccessFileInStream(raf)) { rafis =>
        use(SevenZip.openInArchive(ArchiveFormat.ZIP, rafis)) { archive =>
          use(archive.getSimpleInterface) { smpArchive =>

//            val items = archive.getNumberOfItems

            // split the extractions into an in memory one and a temp file one as some archives may be too big for RAM
            val extractionOp = (0 until smpArchive.getNumberOfItems).map(i => {
              val item = smpArchive.getArchiveItem(i)
              if (item.isEncrypted) {
                throw new UnsupportedOperationException("Encrypted Archive!")
              }
              if (item.getSize < inMemoryLimit) (0, i) else (1, i)
            })

            // vram scan in memory
            val inVram = extractionOp.filter(_._1 == 0).map(_._2).toArray[Int]
            new VRamScanner(archive, sec, location, system, scanListener).extract(inVram)


            // tmp location, extract file then scan and delete
            val inTmp = extractionOp.filter(_._1 == 1).map(_._2).toArray[Int]
            new TmpScanner(archive, sec, source, location, system, scanListener).extract(inTmp)

          }
        }
      }
    }


  }


  override def onComplete: Unit = {
  }
}
