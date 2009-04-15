package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunction;

import java.util.regex.Pattern;

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
   * Provides an abstract KernelFunction based on the given pattern.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractKernelFunction(final Pattern pattern) {
    super(pattern);
  }

  /**
   * Provides an abstract KernelFunction.
   * This constructor can be used if the required input pattern is
   * not yet known at instantiation time and will therefore be set later.
   */
  protected AbstractKernelFunction() {
    super();
  }

  public final D similarity(Integer id1, Integer id2) {
    return similarity(getDatabase().get(id1), getDatabase().get(id2));
  }

  public final D similarity(Integer id1, O o2) {
    return similarity(getDatabase().get(id1), o2);
  }
}
