package com.citypay.pan.search.util

import org.scalatest.FlatSpec

class HexSpec extends FlatSpec {

  "A hex value " should "convert to bytes" in {

    val bs = Hex.fromHex("edabeedb")
    assert(bs(0) === 0xed.toByte)
    assert(bs(1) === 0xab.toByte)
    assert(bs(2) === 0xee.toByte)
    assert(bs(3) === 0xdb.toByte)

  }

  it should "create a hex string" in {

    val hex = Hex.toHexString(Array[Int](0xed, 0xab, 0xee, 0xdb).map(_.toByte))
    assert(hex === "EDABEEDB")

  }

}
