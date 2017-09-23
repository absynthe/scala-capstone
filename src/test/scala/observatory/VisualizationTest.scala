package observatory


import observatory.Visualization.distance
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

trait VisualizationTest extends FunSuite with Checkers {

  test("Weighted inverse distance") {
    val location1 = Location(20, 20)
    val combination1 = List((Location(10, 10), 10.0), (Location(30, 30), 30.0)).map {
      case (otherLocation, temperature) => (distance(otherLocation, location1), temperature)
    }
    assert(Visualization.inverseDistanceWeighted(combination1, 3).round === 20)

    val location2 = Location(30, 10)
    val combination2 = List((Location(10, 10), 10.0), (Location(10, 30), 30.0), (Location(10, 30), 20.0)).map {
      case (otherLocation, temperature) => (distance(otherLocation, location2), temperature)
    }

    assert(Visualization.inverseDistanceWeighted(combination2, 3).round === 17)
  }

  test("Predict temperature") {
    assert(Visualization.predictTemperature(List((Location(45.0, -90.0), 10.0), (Location(-45.0, 0.0), 20.0)), Location(0.0, -45.0)).round === 15)
    assert(Visualization.predictTemperature(List((Location(0.0, 0.0), 10.0)), Location(0.0, 0.0)).round === 10)
    assert(Visualization.predictTemperature(List((Location(45.0, -90.0), 0.0), (Location(-45.0, 0.0), 59.028308521858634)), Location(0.0, 0.0)).round === 52)
  }

  test("colorLinearInterpolation") {
    assert(Visualization.colorLinearInterpolation((0, Color(0, 0, 0)), (100, Color(255, 255, 255)), 50) === Color(128, 128, 128))
    assert(Visualization.colorLinearInterpolation((0, Color(0, 0, 0)), (80, Color(255, 255, 255)), 10) === Color(32, 32, 32))
    assert(Visualization.colorLinearInterpolation((0, Color(255, 128, 0)), (80, Color(0, 128, 255)), 10) === Color(223, 128, 32))
  }

  test("interpolateColor") {
    val palette = List(
      (100.0, Color(255, 255, 255)),
      (50.0, Color(0, 0, 0)),
      (0.0, Color(255, 0, 128))
    )

    assert(Visualization.interpolateColor(palette, 50.0) === Color(0, 0, 0))
    assert(Visualization.interpolateColor(palette, 0.0) === Color(255, 0, 128))
    assert(Visualization.interpolateColor(palette, -10.0) === Color(255, 0, 128))
    assert(Visualization.interpolateColor(palette, 200.0) === Color(255, 255, 255))
    assert(Visualization.interpolateColor(palette, 75.0) === Color(128, 128, 128))
    assert(Visualization.interpolateColor(palette, 25.0) === Color(128, 0, 64))
    assert(Visualization.interpolateColor(List((-9.385071236394225, Color(255, 0, 0)), (1.0, Color(0, 0, 255))), 1.0) === Color(0,0,255))
  }

  test("Visualization") {
    val palette = List(
      (60.0, Color(255, 255, 255)),
      (32.0, Color(255, 0, 0)),
      (12.0, Color(255, 255, 0)),
      (0.0, Color(0, 255, 255)),
      (-15.0, Color(0, 0, 255)),
      (-27.0, Color(255, 0, 255)),
      (-50.0, Color(33, 0, 107)),
      (-60.0, Color(0, 0, 0))
    )

    val img = Visualization.visualize(List((Location(45.0, -90.0), 0.0), (Location(-45.0, 0.0), 59.028308521858634)), palette)

    img.output(new java.io.File(s"vizTest.png"))

    assert(img.pixels.length === 360 * 180)
  }
}
