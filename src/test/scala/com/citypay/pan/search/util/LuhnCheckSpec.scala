package com.citypay.pan.search.util

import org.scalatest.FlatSpec

/**
  * Created by gary on 30/05/2017.
  */
class LuhnCheckSpec extends FlatSpec {

  "A luhn check" should "check validity" in {

    assert(LuhnCheck("4000000000000001") === false)
    assert(LuhnCheck("4000000000000002") === true)
    assert(LuhnCheck("4000000000000003") === false)
    assert(LuhnCheck("4000000000000004") === false)
    assert(LuhnCheck("4000000000000005") === false)
    assert(LuhnCheck("4000000000000006") === false)
    assert(LuhnCheck("4000000000000007") === false)
    assert(LuhnCheck("4000000000000008") === false)
    assert(LuhnCheck("4000000000000009") === false)
    assert(LuhnCheck("4000000000000000") === false)

    for (i <- 0 to 9) {
      println(s"$i: ${LuhnCheck("401200103714000151" + i)}")
    }

  }

  it should "handle bad data" in {

    // note that only numerics expected
    assert(LuhnCheck("4000-0000-0000-0002") === false)
    assert(LuhnCheck("4000∆∆∆∆∆∆0002") === false)
  }

}
