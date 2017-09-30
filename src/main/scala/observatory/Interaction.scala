package observatory

import com.sksamuel.scrimage.{Image, Pixel}
import scala.math._
import observatory.Visualization._

/**
  * 3rd milestone: interactive visualization
  */
object Interaction {

  /**
    * @param zoom Zoom level
    * @param x    X coordinate
    * @param y    Y coordinate
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(zoom: Int, x: Int, y: Int): Location = {
    val n = 1 << zoom
    val lon_deg = x.toDouble / n * 360.0 - 180.0
    val lat_rad = atan(sinh(Pi * (1.0 - 2.0 * y.toDouble / n)))
    val lat_deg = toDegrees(lat_rad)

    Location(lat_deg, lon_deg)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @param zoom         Zoom level
    * @param x            X coordinate
    * @param y            Y coordinate
    * @return A 256×256 image showing the contents of the tile defined by `x`, `y` and `zooms`
    */
  def tile(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)], zoom: Int, x: Int, y: Int): Image = {
    val imageWidth = 256
    val imageHeight = 256

    val pixels = (0 until imageHeight * imageWidth).map(index => {

      val xPos = (index % imageWidth).toDouble / imageWidth + x // column of image as fraction with offset x
      val yPos = (index / imageHeight).toDouble / imageHeight + y // row of image as fraction with offset y

      interpolateColor(
        colors,
        predictTemperature(
          temperatures,
          tileLocation(zoom, xPos.toInt, yPos.toInt)
        )
      ).pixel(127)
    }).seq
    Image(imageWidth, imageHeight, pixels.toArray)
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    *
    * @param yearlyData    Sequence of (year, data), where `data` is some data associated with
    *                      `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
                           yearlyData: Iterable[(Int, Data)],
                           generateImage: (Int, Int, Int, Int, Data) => Unit
                         ): Unit = {
    val zoomLevels = 0 to 3
    for {
      (year, data) <- yearlyData
      zoom <- zoomLevels
      x <- 0 until 1 << zoom
      y <- 0 until 1 << zoom
    } {
      generateImage(year, zoom, x, y, data)
    }
  }

}
