package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Interface combining spatial primitive distance functions with primitive
 * number distance functions. This allows for optimization in the most common
 * types, while not sacrificing generality to support the others.
 * 
 * In essence, you should use this interface only in specialized optimized
 * codepaths.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public interface SpatialPrimitiveDoubleDistanceFunction<V extends SpatialComparable> extends SpatialPrimitiveDistanceFunction<V, DoubleDistance>, PrimitiveDoubleDistanceFunction<V> {
  /**
   * Computes the distance between the two given MBRs according to this
   * distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this
   *         distance function
   */
  double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2);

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   */
  double doubleCenterDistance(SpatialComparable mbr1, SpatialComparable mbr2);
}