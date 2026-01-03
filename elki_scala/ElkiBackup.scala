

/*
--------------
Passby - or not 
--------------
------------------------
Last updated: 26/12/2026
------------------------

-------------------------------------------------------
Creating a function for ELKI DBSCAN. The center
point from each dbscan is given from the median of the
cluster detected by DBSCAN. The entire purpose of this
function is to replace the slow sklearn DBSCAN version.
--------------------------------------------------------

TODO: -> All the bottom functions need to be
         packaged neatly into a larger function/class

      -> Need to link the function to Housefinder function
         we already have. May need to rewrite the Housefinder
         function in Scala anyway, and package the both together.

      -> Need to sanity check with the sklearn function to ensure
         that the DBSCAN result we get from this and sklearn are consistent

-------------
Useful Links:
-------------

https://stackoverflow.com/questions/56905539/kmeans-usage-in-elki-comprehensive-example

https://stackoverflow.com/questions/23634614/running-dbscan-in-elki

 */

import scala.jdk.CollectionConverters._
import scala.collection.mutable.ArrayBuffer

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN
import de.lmu.ifi.dbs.elki.data.{Clustering, DoubleVector, NumberVector}
import de.lmu.ifi.dbs.elki.data.model.Model
import de.lmu.ifi.dbs.elki.data.{`type` => TYPE}
import de.lmu.ifi.dbs.elki.database.{Database, StaticArrayDatabase}
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction
import de.lmu.ifi.dbs.elki.index.tree.metrical.covertree.SimplifiedCoverTree

object ElkiDbscan {

  final case class ClusterCenter(
    clusterIndex: Int,
    size: Int,
    center: (Double, Double) // (lat, lon) or (x, y) - you decide
  )

  private def median(xs: Array[Double]): Double = {
    if (xs.isEmpty) throw new IllegalArgumentException("Cannot take median of empty array.")
    val sorted = xs.sorted
    val n = sorted.length
    if (n % 2 == 1) sorted(n / 2)
    else {
      val hi = sorted(n / 2)
      val lo = sorted(n / 2 - 1)
      (lo + hi) / 2.0
    }
  }

  private def createDatabase(
    data: Array[Array[Double]],
    distance: NumberVectorDistanceFunction[NumberVector]
  ): Database = {
    val indexFactory = new SimplifiedCoverTree.Factory[NumberVector](distance, 1.3, 20)
    val db = new StaticArrayDatabase(
      new ArrayAdapterDatabaseConnection(data),
      java.util.Arrays.asList(indexFactory)
    )
    db.initialize()
    db
  }

  /**
    * Run DBSCAN and return a center per (non-noise) cluster.
    *
    * @param data array of points, each point is Array[Double](dim)
    * @param distance distance function (e.g. SquaredEuclideanDistanceFunction.STATIC)
    * @param epsilon eps
    * @param minPts minpts
    * @param latIdx which dimension is latitude (or "x")
    * @param lonIdx which dimension is longitude (or "y")
    */
  def clusterCentersByMedian(
    data: Array[Array[Double]],
    distance: NumberVectorDistanceFunction[NumberVector],
    epsilon: Double,
    minPts: Int,
    latIdx: Int = 0,
    lonIdx: Int = 1
  ): Seq[ClusterCenter] = {
    require(data.nonEmpty, "data must be non-empty")
    require(latIdx >= 0 && lonIdx >= 0, "indices must be >= 0")

    val db = createDatabase(data, distance)
    val rel = db.getRelation(TYPE.TypeUtil.NUMBER_VECTOR_FIELD)

    val algo = new DBSCAN[DoubleVector](distance, epsilon, minPts)
    val clustering: Clustering[Model] = algo.run(db)

    clustering.getAllClusters.asScala.zipWithIndex.flatMap { case (cluster, idx) =>
      // ELKI typically has "Noise" clusters too; filter those out by name/model
      val name = cluster.getNameAutomatic
      if (name != "Cluster") None
      else {
        val iter = cluster.getIDs.iter()
        val lats = ArrayBuffer[Double]()
        val lons = ArrayBuffer[Double]()

        while (iter.valid) {
          val v = rel.get(iter)
          lats += v.doubleValue(latIdx)
          lons += v.doubleValue(lonIdx)
          iter.advance()
        }

        if (lats.isEmpty) None
        else {
          val latMed = median(lats.toArray)
          val lonMed = median(lons.toArray)
          Some(ClusterCenter(idx, lats.size, (latMed, lonMed)))
        }
      }
    }.toSeq
  }
}


///*
//-----
//Olvin
//-----
//------------------------
//Last updated: 16/10/2020
//------------------------
//
//-------------------------------------------------------
//Creating a function for ELKI DBSCAN. The center
//point from each dbscan is given from the median of the
//cluster detected by DBSCAN. The entire purpose of this
//function is to replace the slow sklearn DBSCAN version.
//--------------------------------------------------------
//
//TODO: -> All the bottom functions need to be
//         packaged neatly into a larger function/class
//
//      -> Need to link the function to Housefinder function
//         we already have. May need to rewrite the Housefinder
//         function in Scala anyway, and package the both together.
//
//      -> Need to sanity check with the sklearn function to ensure
//         that the DBSCAN result we get from this and sklearn are consistent
//
//-------------
//Useful Links:
//-------------
//
//https://stackoverflow.com/questions/56905539/kmeans-usage-in-elki-comprehensive-example
//
//https://stackoverflow.com/questions/23634614/running-dbscan-in-elki
//
// */
//
//import scala.collection.JavaConverters._
//import scala.collection.JavaConverters._
//import scala.collection.mutable.ArrayBuffer
//
///* Libraries imported from the ELKI library - https://elki-project.github.io/releases/current/doc/overview-summary.html */
//
//import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansElkan
//import de.lmu.ifi.dbs.elki.data.model.{ClusterModel, DimensionModel, KMeansModel, Model}
//import de.lmu.ifi.dbs.elki.database.{Database, StaticArrayDatabase}
//import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection
//import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction
//import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN
//import de.lmu.ifi.dbs.elki.database.relation.Relation
//import de.lmu.ifi.dbs.elki.data.NumberVector
//import de.lmu.ifi.dbs.elki.database.Database
//import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection
//import de.lmu.ifi.dbs.elki.logging.CLISmartHandler
////
////import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans.Parameterizer
////import de.lmu.ifi.dbs.elki.math.random.RandomFactory
//
//import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN
//import de.lmu.ifi.dbs.elki.data.{Clustering, DoubleVector, NumberVector}
//import de.lmu.ifi.dbs.elki.database.{Database, StaticArrayDatabase}
//import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection
//import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction
//import de.lmu.ifi.dbs.elki.index.tree.metrical.covertree.SimplifiedCoverTree
//import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction
//import de.lmu.ifi.dbs.elki.data.model
//import de.lmu.ifi.dbs.elki.database.ids.DBIDIter
//import de.lmu.ifi.dbs.elki.data.{`type`=>TYPE} // Need to import in this way as 'type' is a class method in Scala
//
//import org.apache.spark.SparkContext
//import org.apache.spark.SparkConf
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.sql.functions._
//
//val conf =
//  new SparkConf()
//    .setAppName( "temp1" )
//    .setMaster( "local" )
//    .set( "spark.driver.host", "localhost" )
//
//val sc =
//  SparkContext
//    .getOrCreate( conf )
////
///* Initialize spark conditions and read tables  */
//val spark = SparkSession.builder.appName("ELKI5").config("spark.master", "local").getOrCreate()
//val ParquetTable = spark.read.format("parquet").option("inferSchema", "true").option("header", "true").option("mode", "PERMISSIVE").load("/Users/sang/Desktop/ELKIScala/input.parquet")
//val dayTable = spark.read.format("parquet").option("inferSchema", "true").option("header", "true").load("/Users/sang/Desktop/ELKIScala/DayInput.parquet")
//val LatLong = ParquetTable.select("latitude_visit", "longitude_visit")
//val rowDF = LatLong.select(array(LatLong.columns.map(col):_*) as "row")
//val mat = rowDF.collect.map(_.getSeq[Float](0).toArray)
//val matDouble = mat.map(_.map(_.toDouble))
//
///* DBSCAN Hyperparameters */
//val numberOfClustersForDemand = 10
//val nClusters = numberOfClustersForDemand
//val nIters = 1000
//
///* Sample data to detect the DBSCAN for - need to compare this with the sklearn function */
//val data: Array[Array[Double]] = Array.ofDim[Double](1000, 2) // Example dataset
//
///* Define the ELKI Part */
///* Function to create the initial database */
//def createDatabase(data: Array[Array[Double]], distanceFunction: NumberVectorDistanceFunction[NumberVector]): Database = {
//  val indexFactory = new SimplifiedCoverTree.Factory[NumberVector](distanceFunction, 1.3, 20)
//  // Create a database
//  val db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data), java.util.Arrays.asList(indexFactory))
//  // Load the data into the database
//  db.initialize()
//  db
//}
//
///* Creating the relational database for elki  - We define the epsilon and minpt values here.
// *   I've initialized the epsilon and minpts values.
// *  */
//def dbscanClustering(data: Array[Array[Double]], distanceFunction: NumberVectorDistanceFunction[NumberVector], epsilon: Double = 0.01, minpts: Int = 5) = {
//  // Allocate Lat and Long median variables to allocate the medians later
//  var LatMedian = 0.0 // Lat
//  var LongMedian = 0.0 // Long
//
//  // Use the same `distanceFunction` for the database and DBSCAN <- is it required??
//  val db = createDatabase(data, distanceFunction)
//  val rel = db.getRelation(TYPE.TypeUtil.NUMBER_VECTOR_FIELD) // Create the required relational database
//  val dbscan = new DBSCAN[DoubleVector](distanceFunction, epsilon, minpts) // Epsilon and minpoints needed
//  val result: Clustering[Model] = dbscan.run(db)
//  result.getAllClusters.asScala.zipWithIndex.foreach { case (cluster, idx) =>
//    /* Isolate only the clusters and store the median from the DBSCAN results */
//    if (cluster.getNameAutomatic == "Cluster") {
//      // println(s"# $idx: ${cluster.getNameAutomatic}")
//      //println(s"Size: ${cluster.size()}")
//      //println(s"Model: ${cluster.getModel}")
//      //println(s"ids: ${cluster.getIDs.iter().toString}")
//      val DBIDIter = cluster.getIDs.iter()
//      val iter = cluster.getIDs.iter()
//      var counter = 0 // Indexing the number of datapoints allocated from DBSCAN
//      val ab = ArrayBuffer[DoubleVector]()
//      val LongArray = ArrayBuffer[Double]() // Store Long
//      val LatArray = ArrayBuffer[Double]() // Store Lat
//      while (iter.valid) {
//        //println(s"DBids: ${rel.get(iter)} ${counter}") // Print out
//        ab += rel.get(iter) // Append
//        LongArray += ab(counter).doubleValue(0) // Append Lat values
//        LatArray += ab(counter).doubleValue(1) // Append Long Values
//        println(counter + " " + ab(counter).doubleValue(0) + " " + ab(counter).doubleValue(1))
//        counter += 1
//        // Store lat and longs in separate lists and compute the medians
//        iter.advance
//      }
//      // Compute medians
//      LongMedian = LongArray.sortWith(_ < _).drop(LongArray.length / 2).head // Allocate Long median
//      LatMedian = LatArray.sortWith(_ < _).drop(LatArray.length / 2).head // Allocate Lat median
//    }
//  }
//  println("The Center of the cluster from DBSCAN (median) is: "  + LongMedian + " "  + LatMedian)
//  (LongMedian, LatMedian) // Return tuple of point
//}
//
///*  OUTPUT  */
//val Result = dbscanClustering(matDouble, SquaredEuclideanDistanceFunction.STATIC)
//println(Result._1, Result._2) // Need to return the result of tuples
//dayTable.show()
