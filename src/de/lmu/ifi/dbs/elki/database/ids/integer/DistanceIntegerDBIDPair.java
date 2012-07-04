package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Class storing a double distance a DBID.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
class DistanceIntegerDBIDPair<D extends Distance<D>> implements DistanceDBIDPair<D>, IntegerDBIDRef {
  /**
   * The distance value
   */
  D distance;

  /**
   * The integer DBID
   */
  int id;

  /**
   * Constructor.
   * 
   * @param distance Distance
   * @param id Object ID
   */
  protected DistanceIntegerDBIDPair(D distance, int id) {
    super();
    this.distance = distance;
    this.id = id;
  }

  @Override
  public D getDistance() {
    return distance;
  }

  @Override
  public DBIDRef deref() {
    return this;
  }

  @Override
  public int getIntegerID() {
    return id;
  }

  @Override
  public int compareByDistance(DistanceDBIDPair<D> o) {
    return distance.compareTo(o.getDistance());
  }

  @Override
  public String toString() {
    return distance.toString() + ":" + id;
  }

  @Override
  public boolean equals(Object o) {
    if(o instanceof DistanceIntegerDBIDPair) {
      DistanceIntegerDBIDPair<?> p = (DistanceIntegerDBIDPair<?>) o;
      return (this.id == p.id) && distance.equals(p.getDistance());
    }
    if(o instanceof DoubleDistanceIntegerDBIDPair && distance instanceof DoubleDistance) {
      DoubleDistanceIntegerDBIDPair p = (DoubleDistanceIntegerDBIDPair) o;
      return (this.id == p.id) && (((DoubleDistance) this.distance).doubleValue() == p.distance);
    }
    return false;
  }
}