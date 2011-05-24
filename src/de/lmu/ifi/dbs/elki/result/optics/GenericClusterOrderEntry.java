package de.lmu.ifi.dbs.elki.result.optics;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Provides an entry in a cluster order.
 * 
 * @author Elke Achtert
 * @param <D> the type of Distance used by the ClusterOrderEntry
 */
public class GenericClusterOrderEntry<D extends Distance<D>> implements Comparable<ClusterOrderEntry<D>>, ClusterOrderEntry<D> {
  /**
   * The id of the entry.
   */
  private DBID objectID;

  /**
   * The id of the entry's predecessor.
   */
  private DBID predecessorID;

  /**
   * The reachability of the entry.
   */
  private D reachability;

  /**
   * Creates a new entry in a cluster order with the specified parameters.
   * 
   * @param objectID the id of the entry
   * @param predecessorID the id of the entry's predecessor
   * @param reachability the reachability of the entry
   */
  public GenericClusterOrderEntry(DBID objectID, DBID predecessorID, D reachability) {
    this.objectID = objectID;
    this.predecessorID = predecessorID;
    this.reachability = reachability;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   * 
   * NOTE: for the use in an UpdatableHeap, only the ID is compared!
   * 
   * @param o the reference object with which to compare.
   * @return <code>true</code> if this object has the same attribute values as
   *         the o argument; <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || !(o instanceof ClusterOrderEntry)) {
      return false;
    }

    final ClusterOrderEntry<?> that = (ClusterOrderEntry<?>) o;
    // Compare by ID only, for UpdatableHeap!
    return objectID.equals(that.getID());
  }

  /**
   * Returns a hash code value for the object.
   * 
   * @return the object id if this entry
   */
  @Override
  public int hashCode() {
    return objectID.hashCode();
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return objectID + "(" + predecessorID + "," + reachability + ")";
  }

  /**
   * Returns the object id of this entry.
   * 
   * @return the object id of this entry
   */
  @Override
  public DBID getID() {
    return objectID;
  }

  /**
   * Returns the id of the predecessor of this entry if this entry has a
   * predecessor, null otherwise.
   * 
   * @return the id of the predecessor of this entry
   */
  @Override
  public DBID getPredecessorID() {
    return predecessorID;
  }

  /**
   * Returns the reachability distance of this entry
   * 
   * @return the reachability distance of this entry
   */
  @Override
  public D getReachability() {
    return reachability;
  }

  @Override
  public int compareTo(ClusterOrderEntry<D> o) {
    int delta = this.getReachability().compareTo(o.getReachability());
    if(delta != 0) {
      return delta;
    }
    return -getID().compareTo(o.getID());
  }
}