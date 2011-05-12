package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Optimized DistanceResultPair that avoids/postpones an extra layer of boxing
 * for double values.
 * 
 * @author Erich Schubert
 */
public class DoubleDistanceResultPair implements DistanceResultPair<DoubleDistance> {
  /**
   * Distance value
   */
  double distance;

  /**
   * Object ID
   */
  DBID id;

  /**
   * Constructor.
   *
   * @param distance Distance value
   * @param id Object ID
   */
  public DoubleDistanceResultPair(double distance, DBID id) {
    super();
    this.distance = distance;
    this.id = id;
  }

  @Override
  public DoubleDistance getDistance() {
    return new DoubleDistance(distance);
  }

  @Override
  public void setDistance(DoubleDistance distance) {
    this.distance = distance.doubleValue();
  }

  @Override
  public DBID getDBID() {
    return id;
  }

  @Override
  public void setID(DBID id) {
    this.id = id;
  }

  @Override
  public int compareTo(DistanceResultPair<DoubleDistance> o) {
    if(o instanceof DoubleDistanceResultPair) {
      DoubleDistanceResultPair od = (DoubleDistanceResultPair) o;
      final int delta = Double.compare(distance, od.distance);
      if(delta != 0) {
        return delta;
      }
    }
    else {
      final int delta = Double.compare(distance, o.getDistance().doubleValue());
      if(delta != 0) {
        return delta;
      }
    }
    return id.compareTo(o.getDBID());
  }

}
