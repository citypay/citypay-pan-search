package com.citypay.pan.search.io


case class FileSignature(name: String, extensions: List[String], signature: Array[Byte], offset: Int) {

  /**
    * Checks to see if a signature matches
    *
    * @param arr array to test against the signature
    * @return true if the full signature byte length
    */
  //noinspection ScalaStyle allow return values to escape for loop
  def matches(arr: Array[Byte]): Boolean = {
    for (i <- signature.indices) {
      if (i > arr.length) {
        return false
      } else if (signature(i) != arr(i)) {
        return false
      }
    }
    true
  }

}
