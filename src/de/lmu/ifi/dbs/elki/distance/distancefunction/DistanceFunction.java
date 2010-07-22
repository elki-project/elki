package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Base interface for any kind of distances.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance result type
 */
public interface DistanceFunction<O, D extends Distance<D>> {
  /**
   * Method to get the distance functions factory.
   * 
   * @return Factory for distance objects
   */
  D getDistanceFactory();

  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();

  /**
   * Is this distance function metric (in particular, does it satisfy the
   * triangle equation?)
   * 
   * @return {@code true} when metric.
   */
  boolean isMetric();

  /**
   * Get the input data type of the function.
   */
  Class<? super O> getInputDatatype();
}