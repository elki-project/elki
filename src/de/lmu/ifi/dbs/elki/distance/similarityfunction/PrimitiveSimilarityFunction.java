package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface SimilarityFunction describes the requirements of any similarity
 * function.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.uses DatabaseObject oneway - - defined on
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public interface PrimitiveSimilarityFunction<O, D extends Distance<D>> extends SimilarityFunction<O, D> {
  /**
   * Computes the similarity between two given DatabaseObjects according to this
   * similarity function.
   * 
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the similarity between two given DatabaseObjects according to this
   *         similarity function
   */
  D similarity(O o1, O o2);

  @Override
  abstract public SimpleTypeInformation<? super O> getInputTypeRestriction();
}