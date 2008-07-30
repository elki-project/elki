package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.Distance;

import java.util.regex.Pattern;

/**
 * AbstractDistanceFunction provides some methods valid for any extending
 * class.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractDistanceFunction<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractMeasurementFunction<O, D> implements DistanceFunction<O, D> {
    
  /**
   * Provides an abstract DistanceFunction based on the given pattern.
   *
   * @param pattern a pattern to define the required input format
   */
  protected AbstractDistanceFunction(Pattern pattern) {
    super(pattern);
  }

  /**
   * Provides an abstract DistanceFunction.
   * This constructor can be used if the required input pattern is
   * not yet known at instantiation time and will therefore be set later.
   */
  protected AbstractDistanceFunction() {
    super();
  }

  /**
   * Returns the distance between the two objcts specified by their obejct ids.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  public D distance(Integer id1, Integer id2) {
    return distance(getDatabase().get(id1), getDatabase().get(id2));
  }

  /**
   * @see de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction#distance(Integer, Integer)
   */
  public D distance(Integer id1, O o2) {
    return distance(getDatabase().get(id1), o2);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction#isInfiniteDistance(de.lmu.ifi.dbs.elki.distance.Distance)
   */
  public final boolean isInfiniteDistance(D distance) {
    return distance.equals(infiniteDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction#isNullDistance(de.lmu.ifi.dbs.elki.distance.Distance)
   */
  public final boolean isNullDistance(D distance) {
    return distance.equals(nullDistance());
  }

  /**
   * @see de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction#isUndefinedDistance(de.lmu.ifi.dbs.elki.distance.Distance)
   */
  public final boolean isUndefinedDistance(D distance) {
    return distance.equals(undefinedDistance());
  }
}
