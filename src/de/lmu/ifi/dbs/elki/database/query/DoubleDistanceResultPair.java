package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
  public int compareByDistance(DistanceResultPair<DoubleDistance> o) {
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
    return 0;
  }

  @Override
  public int compareTo(DistanceResultPair<DoubleDistance> o) {
    final int delta = this.compareByDistance(o);
    if(delta != 0) {
      return delta;
    }
    return id.compareTo(o.getDBID());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DistanceResultPair)) {
      return false;
    }
    if(obj instanceof DoubleDistanceResultPair) {
      DoubleDistanceResultPair ddrp = (DoubleDistanceResultPair) obj;
      return distance == ddrp.distance && id.equals(ddrp.id);
    }
    DistanceResultPair<?> other = (DistanceResultPair<?>) obj;
    return other.getDistance().equals(distance) && id.equals(other.getDBID());
  }

  /**
   * Get the distance as double value.
   * 
   * @return distance value
   */
  public double getDoubleDistance() {
    return distance;
  }

  @Override
  public String toString() {
    return "DistanceResultPair(" + distance + ", " + id + ")";
  }
}