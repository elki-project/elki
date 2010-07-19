package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
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

  @Override
  public D distance(DBID id1, DBID id2) {
    return distance(getDatabase().get(id1), getDatabase().get(id2));
  }

  @Override
  public D distance(DBID id1, O o2) {
    return distance(getDatabase().get(id1), o2);
  }

  @Override
  public D distance(O o1, DBID id2) {
    return distance(o1, getDatabase().get(id2));
  }

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