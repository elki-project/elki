package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.CPair;

/**
 * Class that consists of a pair (distance, object ID) commonly returned
 * for kNN and range queries.
 * 
 * @author Erich Schubert
 *
 * @param <D> Distance type
 */
public class DistanceResultPair<D extends Distance<D>> extends CPair<D, Integer> {
  /**
   * Canonical constructor
   * 
   * @param first Distance
   * @param second Object ID
   */
  public DistanceResultPair(D first, Integer second) {
    super(first, second);
  }

  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public final D getDistance() {
    return first;
  }

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public final void setDistance(D first) {
    this.first = first;
  }

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public final Integer getID() {
    return second;
  }

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public final void setID(Integer second) {
    this.second = second;
  }
}
