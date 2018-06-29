package com.citypay.pan.search

import org.scalatest.FlatSpec

class InspectionScannerSpec extends FlatSpec {

  "An InspectionScanner" should "check for prefixes delimiters" in {

    for (elem <- 0x0 to 0x2F) assert(InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- '0' to '9') assert(!InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- 0x3A to 0x40) assert(InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- 'A' to 'Z') assert(!InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- 0x5B to 0x60) assert(InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- 'a' to 'z') assert(!InspectionScanner.isPrefixDelimiter(elem.toByte))
    for (elem <- 0x7B to 0x7F) assert(InspectionScanner.isPrefixDelimiter(elem.toByte))

  }

}
