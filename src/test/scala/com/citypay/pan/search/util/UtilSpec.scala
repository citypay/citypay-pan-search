package com.citypay.pan.search.util

import org.scalatest.FlatSpec

/**
  * Created by gary on 01/07/2017.
  */
class UtilSpec extends FlatSpec {

  import Util._

  "DigitsInNumber" should "calculate the number of digits in a number" in {

    assert(1 === DigitsInNumber(1))
    assert(1 === DigitsInNumber(-1))
    assert(2 === DigitsInNumber(12))
    assert(3 === DigitsInNumber(123))
    assert(4 === DigitsInNumber(1234))
    assert(5 === DigitsInNumber(12345))

  }

  "FirstDigit" should "return the first digit of a numeric" in {

    assert(1 === FirstDigit(1234))
    assert(1 === FirstDigit(12345))
    assert(5 === FirstDigit(5555))
    assert(-5 === FirstDigit(-5555))

  }

}
