package com.citypay.pan.search

import com.citypay.pan.search.util.Location


/**
  * A match which is found by inspection
  *
  * @param searchSpec    the spec that is matching
  * @param result        a text result of the inspection
  * @param system        the system this result is running on
  * @param location      the location on the system such as file, database table etc
  * @param from          the location where the search was found from
  * @param to            the location where the search was found to
  * @param expectedValue the expected value of the match
  */
case class Match(searchSpec: PanSpec,
                 result: String,
                 system: String,
                 from: Location,
                 to: Location,
                 offset: Int,
                 location: String,
                 expectedValue: Array[Byte]) extends MatchResult {

  def expectedLen: Int = expectedValue.length

  def maskedValue: String = {
    val s = "*" * (expectedValue.length - 10)
    new String(expectedValue, 0, 6) + s + new String(expectedValue, expectedValue.length - 4, 4)
  }

  def value = new String(expectedValue)

}