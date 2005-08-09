package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

/**
 * Defines the requirements for a distance function that can used in spatial index
 * to measure the dissimilarity between spatial data objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialDistanceFunction extends DistanceFunction {

  /**
   * Computes the distance between the two given SpatialData objects
   * according to this distance function.
   *
   * @param o1 first SpatialData object
   * @param o2 second SpatialData object
   * @return the distance between the two given SpatialData objects according to
   *         this distance function
   */
//  Distance distance(SpatialData o1, SpatialData o2);

  /**
   * Computes the minimum distance between the given MBR and the RealVector object
   * according to this distance function.
   *
   * @param mbr the MBR object
   * @param o   the RealVector object
   * @return the minimum distance between the given MBR and the SpatialData object
   *         according to this distance function
   */
  Distance minDist(MBR mbr, RealVector o);

  /**
   * Computes the distance between the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the two given MBRs according to this distance function
   */
  Distance distance(MBR mbr1, MBR mbr2);

  /**
   * Computes the distance between the centroids of the two given MBRs
   * according to this distance function.
   *
   * @param mbr1 the first MBR object
   * @param mbr2 the second MBR object
   * @return the distance between the centroids of the two given MBRs
   *         according to this distance function
   */
  Distance centerDistance(MBR mbr1, MBR mbr2);
}
