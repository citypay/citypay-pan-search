package com.citypay.pan.search

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.FlatSpec

/**
  * Created by gary on 08/06/2017.
  */
class PanSpecSpec extends FlatSpec {
//
//  "A PanSpec" should "load leading value" in {
//
//
//    assert(PanSpec.leading(4) === 4)
//    assert(PanSpec.leading(44) === 44)
//    assert(PanSpec.leading(443) === 443)
//    assert(PanSpec.leading(4445) === 4445)
//    assert(PanSpec.leading(-44) === 44)
//    assert(PanSpec.leading(.44) === 0)
//
//  }
//
//  it should "load length value" in {
//
//
//    assert(PanSpec.length(4) === 16)
//    assert(PanSpec.length(44) === 16)
//    assert(PanSpec.length(444) === 16)
//    assert(PanSpec.length(44.15) === 15)
//    assert(PanSpec.length(44.16) === 16)
//    assert(PanSpec.length(44.17) === 17)
//    assert(PanSpec.length(44.18) === 18)
//    assert(PanSpec.length(44.19) === 19)
//    assert(PanSpec.length(44.51) === 16)
//    assert(PanSpec.length(44.1) === 10)
//    assert(PanSpec.length(44.0) === 16)
//    assert(PanSpec.length(-44.15) === 16)
//    assert(PanSpec.length(-44.18) === 16)
//
//
//  }


  it should "load from config" in {

    val panSpec = PanSpec.load(ConfigFactory.load("pan-config"), 1)
    assert(panSpec.size === 7)

    assert(panSpec.exists(_.leading == 45))
    assert(panSpec.exists(_.leading == 51))
    assert(panSpec.exists(_.leading == 37))

  }

}
