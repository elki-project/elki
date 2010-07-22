package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface SimilarityFunction describes the requirements of any similarity
 * function.
 *
 * @author Elke Achtert 
 * @param <O> object type
 * @param <D> distance type
 */
public interface SimilarityFunction<O, D extends Distance<D>> extends Parameterizable {
  /**
   * Is this function symmetric?
   * 
   * @return {@code true} when symmetric
   */
  boolean isSymmetric();  

  /**
   * Get the input data type of the function.
   */
  Class<? super O> getInputDatatype();

  /**
   * Get a distance factory.
   * 
   * @return distance factory
   */
  D getDistanceFactory();
}