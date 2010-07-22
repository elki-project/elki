package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractDistanceFunction provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractPrimitiveDistanceFunction<O, D extends Distance<D>> implements PrimitiveDistanceFunction<O, D> {
  /**
   * Provides an abstract DistanceFunction.
   */
  protected AbstractPrimitiveDistanceFunction() {
    // EMPTY
  }

  @Override
  abstract public D distance(O o1, O o2);
  
  @Override
  abstract public D getDistanceFactory();

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }

  @Override
  public boolean isMetric() {
    // Do NOT assume triangle equation by default!
    return false;
  }
}