package de.lmu.ifi.dbs.elki.index.tree.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Encapsulates the attributes for a object that can be stored in a heap. The
 * object to be stored represents a node in a M-Tree and some additional
 * information. Additionally to the regular expansion candidate, this object
 * holds the id of the routing object of the underlying M-Tree node and its
 * covering radius.
 * 
 * @author Elke Achtert
 * 
 * @param <D> the type of Distance used in the M-Tree
 */
public class GenericMTreeDistanceSearchCandidate<D extends Distance<D>> extends GenericDistanceSearchCandidate<D> {
  /**
   * The id of the routing object.
   */
  public DBID routingObjectID;

  /**
   * Creates a new heap node with the specified parameters.
   * 
   * @param mindist the minimum distance of the node
   * @param nodeID the id of the node
   * @param routingObjectID the id of the routing object of the node
   */
  public GenericMTreeDistanceSearchCandidate(final D mindist, final Integer nodeID, final DBID routingObjectID) {
    super(mindist, nodeID);
    this.routingObjectID = routingObjectID;
  }
}