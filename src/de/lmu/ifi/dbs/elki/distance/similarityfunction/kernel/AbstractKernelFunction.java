package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractKernelFunction provides some methods valid for any extending
 * class.
 *
 * @author Elke Achtert 
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractKernelFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPrimitiveDistanceFunction<O, D> implements KernelFunction<O, D> {
  /**
   * Provides an abstract KernelFunction.
   */
  protected AbstractKernelFunction() {
    super();
  }
}