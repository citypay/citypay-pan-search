package com.citypay.pan.search.nio

import java.io.File

import org.scalatest.FlatSpec

class NioFileSystemScannerSpec extends FlatSpec {


  "A file path has excludes" should "exclude files in path" in {


    val ns = new NioFileSystemScanner(Nil, "**",
      exclude = List("/Users/*/Library/Application Support/Google/Chrome/Default/Extensions/**")
    )

    // does not work on other machines due to local file system...
    //
    //    val d = ns.directoryScanStream(
    //      new File("/Users/gary/Library/Application Support/Google/Chrome/Default/Extensions/" +
    //        "pkedcjkdefgpdelpbcmbmeomcjbeemfm/5917.424.0.7_/_locales").toPath
    //    )
    //
    //    val i = d.iterator()
    //    while (i.hasNext) {
    //      println(i.next())
    //    }


  }

}
