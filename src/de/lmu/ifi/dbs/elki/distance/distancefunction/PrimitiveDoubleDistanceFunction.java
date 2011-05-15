package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Interface for distance functions that can provide a raw double value.
 * 
 * This is for use in performance-critical situations that need to avoid the
 * boxing/unboxing cost of regular distance API.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface PrimitiveDoubleDistanceFunction<O> extends PrimitiveDistanceFunction<O, DoubleDistance> {
  /**
   * Computes the distance between two given Objects according to this distance
   * function.
   * 
   * @param o1 first Object
   * @param o2 second Object
   * @return the distance between two given Objects according to this distance
   *         function
   */
  double doubleDistance(O o1, O o2);
}