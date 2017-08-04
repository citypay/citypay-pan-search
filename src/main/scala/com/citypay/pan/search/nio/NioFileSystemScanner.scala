package com.citypay.pan.search.nio

import java.io.File
import java.nio.file._

import com.citypay.pan.search._
import com.citypay.pan.search.io.FileSigReviewFactory
import com.citypay.pan.search.source.ScanSource
import com.citypay.pan.search.util.Util._
import com.citypay.pan.search.util.{Loggable, Timeable}

import scala.collection.JavaConverters._

object NioFileSystemScanner {

  object Stats {
    val Checked = "Checked"
    val Matched = "Matched"
    val Hidden = "Hidden"
    val PermissionFailure = "PermissionFailure"
    val Error = "Error"
  }

}


/**
  * Nio FS spec for scanning
  *
  * @param root               the root path to scan such as /
  * @param include            a glob of what to include
  * @param exclude            a glob of what to exclude
  * @param includeHiddenFiles whether to include hidden files in a search, defaults to false
  * @param recursive          whether the scan should be recursive
  * @param maxDepth           whether the scan should recursively scan directories and to what depth, a value of <= 0 is no depth
  *                           whilst any positive value is considered a depth to consider. A depth of 1 will recurse into a single
  *                           directory
  */
case class NioFileSystemScanner(root: List[String],
                                include: String,
                                exclude: List[String],
                                includeHiddenFiles: Boolean = false,
                                recursive: Boolean = true,
                                maxDepth: Int = -1) extends ScanSource with Loggable with Timeable {

  import NioFileSystemScanner.Stats._

  private var _count = 0
  private val self = this

  override def name: String = "NIO"

  override def close(): Unit = {}

  private def scanFile(system: String, file: File)(implicit sec: ScanExecutionContext): Unit = {

    _count = _count + 1
    sec.queue(self, _listener, new ScanExecutor {

      lazy val factory = new FileSigReviewFactory(file, sec, self, system, file.toString, _listener)

      override def scan(): ScanReport = {
        _listener.scanItem(self, file.getName)
        Timed(s"Scan $file") {
          factory.scan()
        }
        sec.report
      }

      override def onComplete: Unit = {
        sec.report.incStat(Checked)
        factory.onComplete
        Thread.`yield`()
      }

    })


  }

  def scannedFileCount: Int = _count


  def scanDirPath(system: String, dir: Path, depth: Int)(implicit sec: ScanExecutionContext): ScanExecutionContext = {

    use(directoryScanStream(dir)) { stream =>

      val isUnderMaxDepth = if (maxDepth <= 0) true else depth < maxDepth

      stream.iterator().asScala.foreach(path => {

        val file = path.toFile
        val fileName = file.getName

        if (!file.canRead) {
          sec.report.incStat(PermissionFailure)
        } else {
          if (file.isFile && (file.isHidden || fileName.startsWith(".")) && includeHiddenFiles) {
            scanFile(system, file)
          } else if (file.isHidden || fileName.startsWith(".")) {
            sec.report.incStat(Hidden)
          } else if (file.isFile && !file.isHidden) {
            scanFile(system, file)
          } else if (file.isDirectory && recursive && isUnderMaxDepth) {
            scanDirPath(system, path, depth + 1)
          }
        }

      })

    }

    sec
  }


  override def submitScan(ctx: SearchContext): List[ScanReport] = {

    _listener.started(self)
    try {

      // analyse scope first so we can gather n out of k items to scan
      val secs = root.map(rootPath => {

        implicit val sec = new ScanExecutionContext(ctx, name, ScanReport(s"File scan $rootPath"))

        Timed(s"scan $rootPath") {
          try {
            scanDirPath(rootPath, new File(rootPath).toPath, 0)
          } finally {
            sec.end()
            log("Awaiting scan completion")
            sec.await()
            log("Completed")
          }
        }

      })

      log(s"Scanned ${_count}")

      secs.map(_.report)

    } finally {
      _listener.complete(self)
    }

  }


  def directoryScanStream(path: Path): DirectoryStream[Path] = {

    val fs: FileSystem = path.getFileSystem
    val includeMatcher: PathMatcher = fs.getPathMatcher("glob:" + include)
    val excludeMatcher: List[PathMatcher] = exclude.map(ex => fs.getPathMatcher("glob:" + ex))

    val filter = new DirectoryStream.Filter[Path]() {


      private def exclude(entry: Path): Boolean = {
        val ex = excludeMatcher.find(m => m.matches(entry))
        ex.fold(false)(m => true)
      }

      override def accept(entry: Path): Boolean =
        includeMatcher.matches(entry.getFileName) && !exclude(entry)
    }


    fs.provider.newDirectoryStream(path, filter)

  }


}


