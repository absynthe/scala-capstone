package observatory

import com.sksamuel.scrimage.Image

import scala.annotation.tailrec
import scala.math._

/**
  * 2nd milestone: basic visualization
  */
object Visualization {

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location     Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Double)], location: Location): Double = {

    val distanceTemperatureCombinations: Iterable[(Double, Double)] = temperatures.map {
      case (otherLocation, temperature) => (distance(otherLocation, location), temperature)
    }

    val bypassPredictions = distanceTemperatureCombinations.filter(_._1 < 1)

    if (bypassPredictions.isEmpty) inverseDistanceWeighted(distanceTemperatureCombinations, power = 3)
    else {
      bypassPredictions.map(_._2).min
    }
  }

  def distance(l1: Location, l2: Location): Double = {
    val earthRadiusKM = 6372.8

    val l1lat = toRadians(l1.lat)
    val l2lat = toRadians(l2.lat)
    val l1lon = toRadians(l1.lon)
    val l2lon = toRadians(l2.lon)
    val greatCircleDistance = acos(sin(l1lat) * sin(l2lat) + cos(l1lat) * cos(l2lat) * cos(abs(l1lon - l2lon)))
    earthRadiusKM * greatCircleDistance
  }

  def inverseDistanceWeighted(distanceTemperatureCombinations: Iterable[(Double, Double)], power: Int): Double = {
    val (weightedSum, inverseWeightedSum) = distanceTemperatureCombinations
      .aggregate((0.0, 0.0))(
        {
          case ((ws, iws), (distance, temp)) =>
            val w = 1 / pow(distance, power)
            (w * temp + ws, w + iws)
        }, {
          case ((wsA, iwsA), (wsB, iwsB)) => (wsA + wsB, iwsA + iwsB)
        }
      )
    weightedSum / inverseWeightedSum
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value  The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Double, Color)], value: Double): Color = {
    @tailrec
    def interpolateRecursive(sortedPoints: List[(Double, Color)], value: Double, estimatedColor: Color): Color = sortedPoints match {
      case p1 :: Nil => estimatedColor
      case p1 :: tail =>
        val p2 = tail.head
        if (p1._1 <= value && value < p2._1) interpolateRecursive(tail, value, colorLinearInterpolation(p1, p2, value))
        else interpolateRecursive(tail, value, estimatedColor)
    }

    val minPoint = points.minBy(_._1)
    val maxPoint = points.maxBy(_._1)

    if( minPoint._1 >= value) minPoint._2
    else if (maxPoint._1 <= value) maxPoint._2
    else{
      //Optimization: only sort if base cases are not applicable
      val sortedPoints = points.toVector.sortBy(_._1)
      interpolateRecursive(sortedPoints.toList, value, sortedPoints.head._2)
    }


    /* Non-tail recursive runs out of memory, but I leave it here for reference
    def interpolateRecursive(sortedPoints: List[(Double, Color)], value: Double): Color = sortedPoints match {
      case Nil => Color(0, 0, 0)
      case p1 :: tail => {
        if (tail.isEmpty || p1._1 > value) p1._2
        else {
          val p2 = tail.head
          if (p2._1 > value) colorLinearInterpolation(p1, p2, value)
          else interpolateRecursive(tail, value)
        }
      }
    }

    val sortedPoints = points.toList.sortBy(_._1)
    interpolateRecursive(sortedPoints, value)*/
  }

  def colorLinearInterpolation(p1: (Double, Color), p2: (Double, Color), targetT: Double): Color = {
    val greenInterpolation = linearInterpolation(p1._1, p1._2.green, p2._1, p2._2.green, targetT)
    val blueInterpolation = linearInterpolation(p1._1, p1._2.blue, p2._1, p2._2.blue, targetT)
    val redInterpolation = linearInterpolation(p1._1, p1._2.red, p2._1, p2._2.red, targetT)
    Color(redInterpolation, greenInterpolation, blueInterpolation)
  }

  def linearInterpolation(t1: Double, v1: Double, t2: Double, v2: Double, targetT: Double): Int =
    (v1 * (1 - (targetT - t1) / (t2 - t1)) + v2 * ((targetT - t1) / (t2 - t1))).round.toInt


  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)]): Image = {
    val imageWidth = 360
    val imageHeight = 180

    def partialGetLocationFromIndex(imageWidth: Int, imageHeight: Int)(pos: Int): Location = {
      val widthFactor = 180 * 2 / imageWidth.toDouble
      val heightFactor = 90 * 2 / imageHeight.toDouble

      val x: Int = pos % imageWidth
      val y: Int = pos / imageWidth

      Location(90 - (y * heightFactor), (x * widthFactor) - 180)
    }

    val getLocationFromIndex = partialGetLocationFromIndex(imageWidth, imageHeight) _

    val pixels = (0 until imageHeight * imageWidth).map {
      index => interpolateColor(
          colors,
          predictTemperature(
            temperatures,
            getLocationFromIndex(index)
          )
        ).pixel
    }.seq

    Image(imageWidth, imageHeight, pixels.toArray)
  }

}

