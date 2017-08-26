package observatory
import org.apache.log4j.{Level, Logger}

object Main extends App {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
}
