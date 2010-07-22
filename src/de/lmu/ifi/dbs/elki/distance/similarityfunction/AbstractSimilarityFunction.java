package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Base implementation of a similarity function.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> implements SimilarityFunction<O, D> {
  /**
   * Constructor.
   */
  protected AbstractSimilarityFunction() {
    super();
  }

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }

  @Override
  abstract public Class<? super O> getInputDatatype();
}