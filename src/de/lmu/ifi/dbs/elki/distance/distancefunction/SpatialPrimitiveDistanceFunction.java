package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * API for a spatial primitive distance function.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector type
 * @param <D> Distance type
 */
public interface SpatialPrimitiveDistanceFunction<V extends FeatureVector<?, ?>, D extends Distance<D>> extends PrimitiveDistanceFunction<V, D> {
  /**
   * Computes the minimum distance between the given MBR and the FeatureVector
   * object according to this distance function.
   * 
   * @param mbr the MBR object
   * @param v the FeatureVector object
   * @return the minimum distance between the given MBR and the FeatureVector
   *         object according to this distance function
   */
  D minDist(HyperBoundingBox mbr, V v);

  /**
   * Computes the distance between the two given MBRs according to this
   * distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this
   *         distance function
   */
  D distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2);

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   * 
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   */
  D centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2);
  
  @Override
  public <T extends V> SpatialDistanceQuery<T, D> instantiate(Relation<T> relation);
}