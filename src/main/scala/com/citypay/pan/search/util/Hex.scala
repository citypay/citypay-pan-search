package com.citypay.pan.search.util

object Hex {

  private val hexChars = "0123456789ABCDEF".toCharArray
  private val hexTable: Array[Byte] = {
    val a = Array.fill(128)(-1)
    "0123456789".foreach(ch => a(ch.toInt) = ch - '0')
    "ABCDEF".foreach(ch => a(ch.toInt) = ch - 'A' + 10)
    "abcdef".foreach(ch => a(ch.toInt) = ch - 'a' + 10)
    a.map(_.toByte)
  }

  def toHexString(arr: Array[Byte]): String = new String(toHex(arr, 0, arr.length))
  def toHexString(arr: Array[Byte], offset: Int, count: Int): String = new String(toHex(arr, offset, count))
  def toHex(arr: Array[Byte]): Array[Char] = toHex(arr, 0, arr.length)


  def toHex(arr: Array[Byte], offset: Int, count: Int): Array[Char] = {
    val res = new Array[Char](count * 2)
    for (i <- offset until (offset + count)) {
      val b = arr(i) & 0xff
      res(2 * (i - offset)) = hexChars(b >> 4)
      res(2 * (i - offset) + 1) = hexChars(b & 0xf)
    }
    res
  }

  private def RestrictHex(str: String): String = {
    str.replaceAll("0x", "").filter(d =>
      (d >= '0' && d <= '9') ||
        (d >= 'a' && d <= 'f') ||
        (d >= 'A' && d <= 'F')
    )
  }

  def fromHex(str: String): Array[Byte] = {
    val t = RestrictHex(str)
    val e = if (t.length % 2 == 0) t else "0" + t // insert 0.
    val dst = new Array[Byte](e.length / 2)
    val a = e.toCharArray
    for (i <- dst.indices) {
      val t1 = a(i << 1)
      val t2 = a((i << 1) + 1)
      val a1 = if (t1 < 128) hexTable(t1.toInt) << 4 else -1
      val a2 = if (t2 < 128) hexTable(t2.toInt) else -1
      dst(i) = (a1 | a2).toByte
    }
    dst
  }



}
