package observatory

import java.time.LocalDate

import org.apache.spark.sql.{Dataset, Encoder, Encoders, SparkSession}
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}

/**
  * 1st milestone: data extraction
  */
object Extraction {

  val spark: SparkSession = SparkSession.builder().appName("Observatory")
    .config("spark.master", "local").getOrCreate()

  import spark.implicits._

  val stnID = StructField("stnID", DataTypes.StringType)
  val wbanID = StructField("wbanID", DataTypes.StringType)
  val latitude = StructField("latitude", DataTypes.DoubleType)
  val longitude = StructField("longitude", DataTypes.DoubleType)
  val month = StructField("month",DataTypes.IntegerType)
  val day = StructField("day",DataTypes.IntegerType)
  val temperature = StructField("temperature",DataTypes.DoubleType)

  val stationsSchema = StructType(Array(stnID, wbanID, latitude, longitude))
  val temperatureSchema = StructType(Array(stnID,wbanID, month, day, temperature ))

  val localDateEncoder: Encoder[LocalDate] = Encoders.javaSerialization[LocalDate]
  val temperatureEncoder: Encoder[Temperature] = Encoders.scalaDouble
  val locationEncoder: Encoder[Location] = Encoders.kryo[Location]
  val finalEncoder: Encoder[(LocalDate, Location, Temperature)] = Encoders.tuple(localDateEncoder, locationEncoder, temperatureEncoder)

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Dataset[(LocalDate, Location, Temperature)] = {

    val stationsResourcePath = getClass.getResource("/stations.csv").getPath.replace("%20"," ")
    val temperaturesResourcePath = getClass.getResource("/1975.csv").getPath.replace("%20"," ")

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

    //convert to RDD to avoid having to use the custom encoder for LocalDate
    joined.map( row => {
      val temperature: Temperature = (row.getAs[Double]("temperature") - 32) * 5/9
      val location: Location = Location(row.getAs[Double]("latitude"), row.getAs[Double]("longitude"))
      val localDate: LocalDate = LocalDate.of(year, row.getAs[Int]("month"), row.getAs[Int]("day"))
      (localDate, location, temperature)
    })(finalEncoder)
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Dataset[(LocalDate, Location, Temperature)]): Dataset[(Location, Temperature)] = {
    records.groupBy("location").mean("temperature").as[(Location, Temperature)]
  }

}
