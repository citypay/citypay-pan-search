package com.citypay.pan.search.io

import org.scalatest.FlatSpec

/**
  * Created by gary on 13/06/2017.
  */
class IndexedByteBufferSpec extends FlatSpec {


  "An IndexedByteBuffer" should "increment it's position on put" in {


    val i = new IndexedByteBuffer(8)

    assert(i.position() === 0)

    i.put(0x30, 16)
    assert(i.position() === 1)

    i.put(0x31, 17)
    assert(i.position() === 2)

    assert(i.lastEnteredIndex === 17)

  }


  it should "reset when required" in {

    val i = new IndexedByteBuffer(8)
    i.put(0x30, 16)
    i.put(0x31, 17)

    assert(i.position() === 2)

    i.reset()
    assert(i.position() === 0)
    assert(i.lastEnteredIndex === 0)

  }

  it should "provide a backing array" in {

    val i = new IndexedByteBuffer(8)
    i.put(0x30, 16)
    i.put(0x31, 17)


    assert(i.array().length === 8)
    assert(i.array()(0) === 0x30)
    assert(i.array()(1) === 0x31)
    assert(i.array()(2) === 0x0)
    assert(i.array()(3) === 0x0)
    assert(i.array()(4) === 0x0)
    assert(i.array()(5) === 0x0)
    assert(i.array()(6) === 0x0)
    assert(i.array()(7) === 0x0)

    assert(i.position() == 2)



  }

  it should "shift left" in {

    var i = new IndexedByteBuffer(8)
    i.put(0x30, 16)
    i.put(0x31, 17)
    assert(i.position() == 2)
    assert(i.lastEnteredIndex === 17)

    i.shiftLeft(1)

    assert(i.array().length === 8)
    assert(i.array()(0) === 0x31)
    assert(i.array()(1) === 0x0)
    assert(i.array()(2) === 0x0)
    assert(i.array()(3) === 0x0)
    assert(i.array()(4) === 0x0)
    assert(i.array()(5) === 0x0)
    assert(i.array()(6) === 0x0)
    assert(i.array()(7) === 0x0)
    assert(i.position() == 1)
    assert(i.lastEnteredIndex === 17)


    i = new IndexedByteBuffer(8)
    i.put(0x30, 16)
    i.put(0x31, 17)
    i.shiftLeft(2)

    assert(i.array().length === 8)
    assert(i.array()(0) === 0x0)
    assert(i.array()(1) === 0x0)
    assert(i.array()(2) === 0x0)
    assert(i.array()(3) === 0x0)
    assert(i.array()(4) === 0x0)
    assert(i.array()(5) === 0x0)
    assert(i.array()(6) === 0x0)
    assert(i.array()(7) === 0x0)
    assert(i.position() == 1)
    assert(i.lastEnteredIndex === 0)


  }




}
