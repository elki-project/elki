package de.lmu.ifi.dbs.index.metrical.mtree.util;

import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;

/**
 * Helper class: encapsulates ta node in a M-Tree and the routing object in its parent node.
 * This class is used for insertion of new data objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ParentInfo<O extends MetricalObject, D extends Distance<D>>{

  /**
   * The node.
   */
  MTreeNode<O, D> node;

  /**
   * The routing object of the node.
   */
  Integer routingObjectID;

  /**
   * Creates a new ParentInfo object with the specified parameters.
   *
   * @param node            the node
   * @param routingObjectID the routing object of the node
   */
  public ParentInfo(MTreeNode<O, D> node, Integer routingObjectID) {
    this.node = node;
    this.routingObjectID = routingObjectID;
  }

  /**
   * Returns the node.
   * @return the node
   */
  public MTreeNode<O, D> getNode() {
    return node;
  }

  /**
   * Returns the routing object of the node.
   * @return the routing object of the node
   */
  public Integer getRoutingObjectID() {
    return routingObjectID;
  }
}
