package com.citypay.pan.search.io

import com.citypay.pan.search.util.Hex
import org.scalatest.FlatSpec

class FileSignatureSpec extends FlatSpec {

  "A file signature" should "analyse the header of a file as a match" in {

    val sig = FileSignatures.sig("Example:eg:0A 0B 0B 0A")
    assert(Hex.toHexString(sig.signature) === "0A0B0B0A")
    val bs = Hex.fromHex("0A 0B 0B 0A")
    assert(sig.matches(bs))

  }

}
