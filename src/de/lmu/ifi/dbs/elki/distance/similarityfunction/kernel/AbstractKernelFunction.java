package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractKernelFunction provides some methods valid for any extending
 * class.
 *
 * @author Elke Achtert 
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractKernelFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceFunction<O, D> implements KernelFunction<O, D> {
  /**
   * Provides an abstract KernelFunction.
   * 
   * @param distance Distance Factory
   */
  protected AbstractKernelFunction(D distance) {
    super(distance);
  }

  @Override
  public final D similarity(Integer id1, Integer id2) {
    return similarity(getDatabase().get(id1), getDatabase().get(id2));
  }

  @Override
  public final D similarity(Integer id1, O o2) {
    return similarity(getDatabase().get(id1), o2);
  }
}
