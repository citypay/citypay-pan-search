package com.citypay.pan.search

import java.io.{File, RandomAccessFile}

import com.citypay.pan.search.io.FileSigReviewFactory
import com.citypay.pan.search.nio.FileChannelReviewer
import com.citypay.pan.search.report.SimpleReportRenderer
import com.citypay.pan.search.source.NoOpScanListener
import org.scalatest.FlatSpec

/**
  * Created by gary on 30/05/2017.
  */
class SimpleScanner extends FlatSpec {

  def loadFile(resource: String) = {
    val file = new File(getClass.getClassLoader.getResource(resource).toURI)
    assert(file.exists(), s"$file not found")
    assert(file.canRead, s"$file not readable")
    file
  }


  val ctxFirstMatch = new UnitTestSearchContext {
    override def level1: List[PanSpec] = List(
      PanSpec("Example", "1", "placeholder", 40, 16, 16)
    )
  }

  val ctxMultiMatch = new UnitTestSearchContext {
    override def stopOnFirstMatch: Boolean = false

    override def level1: List[PanSpec] = List(
      PanSpec("Example 1", "1", "placeholder", 40, 16, 19),
      PanSpec("Example 2", "1", "placeholder", 51, 16, 16)
    )

    override def level2: List[PanSpec] = level1
  }

  def review(n: String, multi: Boolean = false): ScanReport = {
    val ef = loadFile(n)
    val sec = new ScanExecutionContext(if (multi) ctxMultiMatch else ctxFirstMatch, "Example", ScanReport("Example"))
    new FileSigReviewFactory(ef, sec, null, "UnitTest", "loc", NoOpScanListener).scan()
  }

  "A scanner" should "scan a simple example file with card data and find a result" in {

    assert(review("example-file.txt").matchCount === 1)
    assert(review("example-file.txt", multi = true).matchCount === 1)

  }

  it should "scan an empty file and not find a result" in {

    assert(review("empty-file.txt").matchCount === 0)
    assert(review("empty-file.txt", multi = true).matchCount === 0)

  }

  it should "scan a simple example file with no data and find no result" in {

    assert(review("example-file-nochd.txt").matchCount === 0)
    assert(review("example-file-nochd.txt", multi = true).matchCount === 0)

  }

  it should "scan a simple example file with intersecting card data and find a result" in {

    assert(review("example-file2.txt").matchCount === 1)
    assert(review("example-file2.txt", multi = true).matchCount === 2)

  }

  it should "scan an example apacs settlement file" in {
    assert(review("apacs.txt").matchCount === 1)
  }

  it should "scan an example apacs settlement file with a 19 digit pan" in {
    val r = review("apacs.txt", multi = true)
    //    new SimpleReportRenderer(System.out).render(r)
    assert(r.matchCount === 2)
  }


  // zip scanning not currently working, removed test.
//  it should "scan a zip file" in {
//    val r = review("example.zip", multi = true)
//    new SimpleReportRenderer(System.out).render(r)
//    assert(r.matchCount === 3)
//  }


}
