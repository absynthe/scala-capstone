package observatory

import com.sksamuel.scrimage.Pixel

case class Location(lat: Double, lon: Double)

case class Color(red: Int, green: Int, blue: Int) {
  def pixel(alpha: Int): Pixel = Pixel.apply(this.red, this.green, this.blue, alpha)
}

