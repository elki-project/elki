package de.lmu.ifi.dbs.elki.result.optics;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Generic Cluster Order Entry Interface.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.composedOf DBID
 * @apiviz.composedOf Distance
 *
 * @param <D> Distance type
 */
public interface ClusterOrderEntry<D extends Distance<D>> {
  /**
   * Returns the object id of this entry.
   * 
   * @return the object id of this entry
   */
  public DBID getID();

  /**
   * Returns the id of the predecessor of this entry if this entry has a
   * predecessor, null otherwise.
   * 
   * @return the id of the predecessor of this entry
   */
  public DBID getPredecessorID();

  /**
   * Returns the reachability distance of this entry
   * 
   * @return the reachability distance of this entry
   */
  public D getReachability();
}