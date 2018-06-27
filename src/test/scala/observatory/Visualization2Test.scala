package observatory

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait Visualization2Test extends FunSuite with Checkers {
  test("bilinearInterpolation"){

    assert(Visualization2.bilinearInterpolation(0.5, 0.5, 10, 20, 30, 40) === 25.0)
    assert(Visualization2.bilinearInterpolation(0.1, 0.5, 10, 20, 30, 40) === 17.0)
    assert(Visualization2.bilinearInterpolation(0.5, 0.1, 10, 20, 30, 40) === 21.0)
    assert(Visualization2.bilinearInterpolation(0.9, 0.1, 10, 20, 30, 40) === 29.0)
    assert(Visualization2.bilinearInterpolation(1.0, 0.0, 10, 20, 30, 40) === 30.0)
  }
}
