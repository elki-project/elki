package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.pairs.PairInterface;

/**
 * Class that consists of a pair (distance, object ID) commonly returned
 * for kNN and range queries.
 * 
 * @author Erich Schubert
 *
 * @param <D> Distance type
 */
public interface DistanceResultPair<D extends Distance<D>> extends PairInterface<D, DBID>, Comparable<DistanceResultPair<D>> {
  /**
   * Getter for first
   * 
   * @return first element in pair
   */
  public D getDistance();

  /**
   * Setter for first
   * 
   * @param first new value for first element
   */
  public void setDistance(D first);

  /**
   * Getter for second element in pair
   * 
   * @return second element in pair
   */
  public DBID getDBID();

  /**
   * Setter for second
   * 
   * @param second new value for second element
   */
  public void setID(DBID second);

  /**
   * Compare value, but by distance only.
   * 
   * @param o Other object
   * @return comparison result, as by Double.compare(this, other)
   */
  public int compareByDistance(DistanceResultPair<D> o);
}