package com.citypay.pan.search

import com.citypay.pan.search.source.ScanSource
import org.scalatest.FlatSpec

/**
  * Created by gary on 08/06/2017.
  */
class SearchContextSpec extends FlatSpec {

  case class Example(_sources: List[ScanSource] = Nil, _spec: List[PanSpec] = Nil) extends UnitTestSearchContext {
    override def sources = _sources

    override def level1 = _spec

    override def level2 = _spec
  }


  "A search context" should "calculate the leading pan digits" in {

    val ex = Example(Nil, List(
      PanSpec("V", "1", "placeholder", 42, 16, 16),
      PanSpec("V", "1", "placeholder", 44, 16, 16),
      PanSpec("V", "1", "placeholder", 45, 16, 16),
      PanSpec("V", "1", "placeholder", 47, 16, 16),
      PanSpec("MC", "2", "placeholder", 51, 16, 16)
    ))

    assert(ex.leadingPanDigits.size === 2) // 4, 5

  }

  it should "calculte the minimum length" in {

    var ex = Example(Nil, List(
      PanSpec("V", "1", "placeholder", 42, 16, 16),
      PanSpec("V", "1", "placeholder", 44, 12, 16),
      PanSpec("V", "1", "placeholder", 45, 18, 19)
    ))

    assert(ex.minimumLength === 12)

    ex = Example(Nil, List(
      PanSpec("V", "1", "placeholder", 42, 16, 16),
      PanSpec("V", "1", "placeholder", 44, 17, 17),
      PanSpec("V", "1", "placeholder", 45, 18, 18)
    ))

    assert(ex.minimumLength === 16)

  }


}
