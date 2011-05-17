package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Trivial implementation using a generic pair.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericDistanceResultPair<D extends Distance<D>> extends Pair<D, DBID> implements DistanceResultPair<D> {
  /**
   * Canonical constructor
   * 
   * @param first Distance
   * @param second Object ID
   */
  public GenericDistanceResultPair(D first, DBID second) {
    super(first, second);
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  @Override
  public final D getDistance() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  @Override
  public final void setDistance(D first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  @Override
  public final DBID getDBID() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  @Override
  public final void setID(DBID second) {
    this.second = second;
  }

  @Override
  public int compareByDistance(DistanceResultPair<D> o) {
    return first.compareTo(o.getDistance());
  }

  @Override
  public int compareTo(DistanceResultPair<D> o) {
    final int ret = compareByDistance(o);
    if(ret != 0) {
      return ret;
    }
    return second.compareTo(o.getDBID());
  }

  @Override
  public boolean equals(Object obj) {
    if(!(obj instanceof DistanceResultPair)) {
      return false;
    }
    DistanceResultPair<?> other = (DistanceResultPair<?>) obj;
    return first.equals(other.getDistance()) && second.equals(other.getDBID());
  }

  @Override
  public String toString() {
    return "DistanceResultPair(" + getFirst() + ", " + getSecond() + ")";
  }
}