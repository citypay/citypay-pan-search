package com.citypay.pan.search.util

object Location {
  val None = Location(0, 0)
}

case class Location(lineNo: Int, colNo: Int) {
  override def toString: String = s"$lineNo:$colNo"
}
