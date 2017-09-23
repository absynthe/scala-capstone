package observatory

import java.time.LocalDate

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}

/**
  * 1st milestone: data extraction
  */
object Extraction {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  val spark: SparkSession = SparkSession.builder().appName("Observatory")
    .config("spark.executor.memory", "1G")
    .config("spark.master", "local").getOrCreate()

  import spark.implicits._

  val stnID = StructField("stnID", DataTypes.StringType)
  val wbanID = StructField("wbanID", DataTypes.StringType)
  val latitude = StructField("latitude", DataTypes.DoubleType)
  val longitude = StructField("longitude", DataTypes.DoubleType)
  val month = StructField("month", DataTypes.IntegerType)
  val day = StructField("day", DataTypes.IntegerType)
  val temperature = StructField("temperature", DataTypes.DoubleType)

  val stationsSchema = StructType(Array(stnID, wbanID, latitude, longitude))
  val temperatureSchema = StructType(Array(stnID, wbanID, month, day, temperature))


  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Int, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Double)] = {
    val stationsResourcePath = getClass.getResource(stationsFile).getPath
    val temperaturesResourcePath = getClass.getResource(temperaturesFile).getPath

    val stations = spark.read
      .schema(stationsSchema)
      .option("header", value = false)
      .csv(stationsResourcePath)

    val temperatures = spark.read
      .schema(temperatureSchema)
      .option("header", value = false)
      .csv(temperaturesResourcePath)

    val filteredStations = stations.filter("latitude IS NOT NULL and longitude IS NOT NULL")
    val joined = filteredStations.join(temperatures, stations("stnID") <=> temperatures("stnID") &&  stations("wbanID") <=> temperatures("wbanID"))

    joined.rdd.map(row => {
      val temperature: Double = (row.getAs[Double]("temperature") - 32) * 5 / 9
      val location: Location = Location(row.getAs[Double]("latitude"), row.getAs[Double]("longitude"))
      val localDate: LocalDate = LocalDate.of(year, row.getAs[Int]("month"), row.getAs[Int]("day"))
      (localDate, location, temperature)
    }).collect
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Double)]): Iterable[(Location, Double)] = {
    val recordsRDD = spark.sparkContext.parallelize(records.toSeq).map { case (_, location, temp) => (location, temp) }
    val recordsDS = recordsRDD.toDS()
      .withColumnRenamed("_1", "location")
      .withColumnRenamed("_2", "temperature")

    recordsDS.groupBy("location").mean("temperature").as[(Location, Double)].collect
  }

}
