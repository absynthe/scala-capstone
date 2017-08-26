name := course.value ++ "-" ++ assignment.value

scalaVersion := "2.11.8"
val sparkVersion = "2.1.0"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture",
  "-Xexperimental"
)

libraryDependencies ++= Seq(
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.6", // for visualization
  "org.apache.spark" %% "spark-core" % sparkVersion, // I chose to implement the assignment with Spark
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.parquet" % "parquet-format" % "2.3.1",// Necessary to fix error 416 when SBT tries to install Spark dependencies
  "org.scalacheck" %% "scalacheck" % "1.12.1" % Test,
  "junit" % "junit" % "4.10" % Test
)

courseId := "PCO2sYdDEeW0iQ6RUMSWEQ"

parallelExecution in Test := false // So that tests are executed for each milestone, one after the other
