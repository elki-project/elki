package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractDistanceFunction provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <D> the type of Distance used
 */
public abstract class AbstractDistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractMeasurementFunction<O, D> implements DistanceFunction<O, D> {
  /**
   * Provides an abstract DistanceFunction.
   * 
   * @param distance Distance factory
   */
  protected AbstractDistanceFunction(D distance) {
    super(distance);
  }

  /**
   * Returns the distance between the two object specified by their object ids.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two object specified by their object ids
   */
  public D distance(Integer id1, Integer id2) {
    return distance(getDatabase().get(id1), getDatabase().get(id2));
  }

  public D distance(Integer id1, O o2) {
    return distance(getDatabase().get(id1), o2);
  }
}