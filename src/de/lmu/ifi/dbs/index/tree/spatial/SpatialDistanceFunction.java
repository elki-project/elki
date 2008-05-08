package de.lmu.ifi.dbs.index.tree.spatial;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

/**
 * Defines the requirements for a distance function that can used in spatial index
 * to measure the dissimilarity between spatial data objects.
 *
 * @author Elke Achtert 
 */
public interface SpatialDistanceFunction<O extends FeatureVector<O,?>, D extends Distance<D>> extends DistanceFunction<O, D> {

  /**
   * Computes the minimum distance between the given MBR and the NumberVector object
   * according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the NumberVector object
   * @return the minimum distance between the given MBR and the SpatialData object
   *         according to this distance function
   */
  D minDist(HyperBoundingBox mbr, O o);

  /**
   * Computes the minimum distance between the given MBR and the NumberVector object
   * with the given id according to this distance function.
   *
   * @param mbr the MBR object
   * @param id  the id of the NumberVector object
   * @return the minimum distance between the given MBR and the SpatialData object
   *         according to this distance function
   */
  D minDist(HyperBoundingBox mbr, Integer id);

  /**
   * Computes the distance between the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this distance function
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
}
