package com.citypay.pan.search.io

import com.citypay.pan.search.util.Location

case class IndexedPosition(offset: Int, location: Location = Location(0, 0))
object IndexedPosition {
  val Undefined = IndexedPosition(-1)
}

/**
  * A ByteBuffer wrapper which tracks the index of a byte in a channel alongside the byte
  *
  * @param sz the size of the buffer
  */
class IndexedByteBuffer(sz: Int) {

  private var _pos = 0
  private var _offset = 0
  private val inspectionBuffer = new Array[Byte](sz)
  private val inspectionIndex = new Array[IndexedPosition](sz)

  /**
    * Puts the byte with the given index into the buffer
    *
    * @param byte   the byte to add
    * @param index  the index position within the underlying file
    * @param lineNo a line no where the data is found
    * @param colNo  a column no in the line where the data is found
    * @return the position in the buffer where the byte was added
    */
  def put(byte: Byte, index: Int, lineNo: Int = 0, colNo: Int = 0): Int = {
    inspectionBuffer(_pos) = byte
    inspectionIndex(_pos) = IndexedPosition(index, Location(lineNo, colNo))
    _pos = _pos + 1
    _pos - 1
  }

  /**
    * @return the current position in the buffer
    */
  def position(): Int = _pos

  /**
    * @param i the amount if items wanted to be available in the buffer
    * @return the number if items remaining before the buffer is exhausted
    */
  def hasRemaining(i: Int): Int = sz - _pos

  def get(i: Int): Byte = inspectionBuffer(i)

  def toString(offset: Int, len: Int) = new String(inspectionBuffer, offset, len)

  def length: Int = sz - _offset

  /**
    * @return the index value at the given position
    */
  def channelIndex(position: Int): Int = channelIndexPosition(position).offset

  def channelIndexPosition(position: Int): IndexedPosition = Option(inspectionIndex(position)).getOrElse(new IndexedPosition(0, Location.None))

  def channelIndexLineNo(position: Int): Int = channelIndexLocation(position).lineNo

  def channelIndexColNo(position: Int): Int = channelIndexLocation(position).colNo

  def channelIndexLocation(position: Int): Location = Option(inspectionIndex(position)).fold(Location.None)(_.location)


  /**
    * Resets the buffer, position and offset to 0
    */
  def reset(): Unit = {
    if (_pos > 0) {
      // rewind and push the initialisation buffer through
      for (i <- 0 until sz) {
        inspectionBuffer(i) = 0x0
        inspectionIndex(i) = IndexedPosition.Undefined
      }
    }
    _pos = 0
    _offset = 0
  }

  /**
    * Function shifts the entire buffer left by 1 index, essentially marking the data
    * as inspected and freeing up buffer resources
    *
    * @return the current position as per [[position()]]
    */
  def shiftLeft(count: Int): Int = {
    assume(count > 0, "Count should be > 0")

    for (i <- 0 until _pos) {
      if (i > _pos - count) {
        inspectionBuffer(i) = 0x0
        inspectionIndex(i) = IndexedPosition.Undefined
      } else {
        inspectionBuffer(i) = inspectionBuffer(i + count)
        inspectionIndex(i) = inspectionIndex(i + count)
      }

    }
    inspectionIndex(_pos) = IndexedPosition.Undefined
    inspectionBuffer(_pos) = 0x0
    _pos = _pos - 1
    _pos
  }

  def lastEnteredIndex: Int = if (_pos <= 0 || inspectionIndex == null) 0 else {
    val i = inspectionIndex(_pos - 1)
    if (i == null) 0 else i.offset
  }

  def array(): Array[Byte] = {
    val dst = new Array[Byte](sz - _offset)
    System.arraycopy(inspectionBuffer, _offset, dst, 0, dst.length)
    dst
  }

  def incOffset(): Unit = {
    _offset = _offset + 1
  }

  reset()

  override def toString: String = s"IndexedByteBuffer[pos=${_pos}, offset=${_offset}, sz=$sz]"
}
