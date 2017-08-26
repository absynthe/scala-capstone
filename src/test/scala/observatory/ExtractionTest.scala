package observatory

import java.time.LocalDate

import observatory.Extraction.{stationsSchema, temperatureSchema, finalEncoder}
import org.apache.spark.sql.SparkSession
import org.junit.runner.RunWith
import org.scalatest.FunSuite


trait ExtractionTest extends FunSuite {
  test("First test") {
    val spark: SparkSession = SparkSession.builder().appName("Observatory")
      .config("spark.master", "local").getOrCreate()
    import spark.implicits._

    val stationsResourcePath = getClass.getResource("/stations.csv").getPath.replace("%20", " ")
    val temperaturesResourcePath = getClass.getResource("/1975.csv").getPath.replace("%20", " ")

    val stations = spark.read
      .schema(stationsSchema)
      .option("header", value = false)
      .csv(stationsResourcePath)

    val temperatures = spark.read
      .schema(temperatureSchema)
      .option("header", value = false)
      .csv(temperaturesResourcePath)

    val filteredStations = stations.filter("latitude IS NOT NULL and longitude IS NOT NULL")
    val joined = stations.join(temperatures, Seq("stnID", "wbanID"))

    //converting to rdd to avoid having to write custom encoder for LocalDate
    val formatted = joined.map(row => {
      val temperature = (row.getAs[Double]("temperature") - 32) * 5 / 9
      val location: Location = Location(row.getAs[Double]("latitude"), row.getAs[Double]("longitude"))
      val localDate: LocalDate = LocalDate.of(2017, row.getAs[Int]("month"), row.getAs[Int]("day"))
      (localDate, location, temperature)
    })(finalEncoder)

    formatted.take(10).foreach(println)
  }
}