import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.functions.{col, expr}

val spark = SparkSession.builder.appName("ELKI").getOrCreate()
val df3 = spark.read.format("parquet").option("inferSchema", "true").option("header", "true").option("mode", "PERMISSIVE").load("input.parquet")
val visit = df3.select("latitude_visit", "longitude_visit")
val rowDF = visit.select(array(visit.columns.map(col):_*) as "row")



