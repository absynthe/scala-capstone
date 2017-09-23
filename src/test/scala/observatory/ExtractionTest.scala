package observatory

import java.time.LocalDate

import observatory.Extraction.{finalEncoder, stationsSchema, temperatureSchema}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Ignore}

@Ignore
trait ExtractionTest extends FunSuite {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

  test("First test") {
    val spark: SparkSession = SparkSession.builder().appName("Observatory")
      .config("spark.master", "local").getOrCreate()

    val stationsResourcePath = getClass.getResource("/stations.csv").getPath.replace("%20", " ")
    val temperaturesResourcePath = getClass.getResource("/1975.csv").getPath.replace("%20", " ")

    import spark.implicits._

    val stations = spark.read
      .schema(stationsSchema)
      .option("header", value = false)
      .csv(stationsResourcePath)

    val temperatures = spark.read
      .schema(temperatureSchema)
      .option("header", value = false)
      .csv(temperaturesResourcePath)

    val filteredStations = stations.filter("latitude IS NOT NULL and longitude IS NOT NULL")
    //val joined = stations.join(temperatures, Seq("stnID", "wbanID"))

    val joined = stations.join(temperatures, stations("stnID") <=> temperatures("stnID") &&  stations("wbanID") <=> temperatures("wbanID"))

    //converting to rdd to avoid having to write custom encoder for LocalDate
    val formatted = joined.map(row => {
      val temperature = (row.getAs[Double]("temperature") - 32) * 5 / 9
      val location: Location = Location(row.getAs[Double]("latitude"), row.getAs[Double]("longitude"))
      val localDate: LocalDate = LocalDate.of(2017, row.getAs[Int]("month"), row.getAs[Int]("day"))
      (localDate, location, temperature)
    })(finalEncoder)

    val t2 = Extraction.locationYearlyAverageRecords(formatted.collect)
  }
}