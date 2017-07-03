package com.citypay.pan.search

import com.citypay.pan.search.util.Util
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * A specification for matching a PAN based on a simplistic model. The spec will only cater for LUHN validated
  * card numbers which may exclude schemes such as Diners from the search.
  *
  * @param name    a name for the specification
  * @param id      an id associated to the type
  * @param logo    an id associated with the type/card scheme
  * @param leading numerics used at the beginning of the search as a prefix, may be 1 or more digits
  * @param length  the expected minimum length of the entire pan
  */
case class PanSpec(name: String,
                   id: String,
                   logo: String,
                   leading: Int,
                   length: Int,
                   maxLength: Int) {

  assume(leading > 0, "Leading digits should be greater than 0")
  assume(length > 0, "Length should be greater than 0")

  private val leadingChars = leading.toString.toCharArray

  val leadingLen: Int = leading.toString.length

  val firstDigit: Int = Util.FirstDigit(leading)

  val leadingBytes: Array[Byte] = leading.toString.getBytes

  /**
    * @param i 0 based index of the leading numerics
    * @return any leading digit at the given index or None if overflowed
    */
  def nLeadingDigit(i: Int): Option[Int] = {
    if (leadingChars.length > i) {
      None
    } else {
      Some(leadingChars(i).toString.toInt)
    }
  }

  private val leadingArr = leading.toString.toCharArray.map(_.toByte)
  //
  //  def matches(position: Int, b: Byte): BinSearchMatcherResult.Value = {
  //    import BinSearchMatcherResult._
  //
  //    // shortcut any length discrepancies
  //    if (position >= lengthMax)
  //      NoMatch
  //
  //    // see if we have a match which is in the leading bracket, leading may (but unlikely) be the length but lets
  //    // check the length for a full match
  //    else if (position < leadingLen) {
  //      if (leadingArr(position) == b)
  //        return if (positionInLenRange(position + 1)) FullMatch else LeadingMatch
  //      NoMatch
  //    }
  //
  //    // other chars should check acceptable chars, a full match however should only appear on a digit
  //    else if (UTF8_CHARS.contains(b))
  //      if (positionInLenRange(position + 1) && Character.isDigit(b)) FullMatch else CharMatch
  //    else
  //      NoMatch
  //
  //  }


}

object PanSpec {

  import com.citypay.pan.search.util.ConfigExt._

  def load(conf: Config, level: Int): List[PanSpec] = {

    val data = conf.getObjectList(s"chd.level$level").asScala.toList.map(c => {
      val cfg = c.toConfig
      (cfg.string("name", ""),
        cfg.string("len", "16"),
        cfg.getString("id"),
        cfg.getString("logo"),
        cfg.getIntList("bins").asScala.toList)
    })

    val flattened = data.flatMap {
      case (n, len, id, logo, list) =>

        val s = len.split("-")
        val (from, to) = if (s.length == 2) (s(0).toInt, s(1).toInt) else (len.toInt, len.toInt)
        list.map(d => PanSpec(n, id, logo, d, from, to))

    }

    flattened

  }

  //  def leading(d: Double): Int = abs(floor(d).toInt)
  //
  //  def length(d: Double): Int = {
  //    val bd = BigDecimal(d)
  //    val len = ((bd - bd.setScale(0, BigDecimal.RoundingMode.HALF_DOWN)) * 100).toInt
  //    if (len <= 0) 16 else len
  //  }

}