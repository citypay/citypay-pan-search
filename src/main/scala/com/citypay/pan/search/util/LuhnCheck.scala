package com.citypay.pan.search.util

/**
  * Performs a LUHN mod 10 check of a card number, see https://en.wikipedia.org/wiki/Luhn_algorithm
  */
object LuhnCheck {

  def apply(char: Array[Char]): Boolean = {
    try {
      val sum = char.map(_.toString.toInt).reverse.grouped(2).map(c => {
        val i = if (c.length > 1) {
          val d = c(1) * 2
          if (d > 9)
            1 + (d % 10)
          else
            d
        }
        else 0
        i + c(0)
      })
      sum.sum % 10 == 0
    } catch {
      case e:NumberFormatException => false
    }
  }

  def apply(str: String): Boolean = apply(str.toCharArray)

}
