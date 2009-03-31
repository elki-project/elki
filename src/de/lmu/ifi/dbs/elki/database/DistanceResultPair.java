package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.CPair;

public class DistanceResultPair<D extends Distance<D>> extends CPair<D, Integer> {
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
