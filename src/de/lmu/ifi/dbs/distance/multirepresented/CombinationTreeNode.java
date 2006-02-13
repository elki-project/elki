package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;

/**
 * Represents a node in a combination tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CombinationTreeNode<O extends DatabaseObject<O>, M extends MultiRepresentedObject<O>, D extends Distance<D>> {
  /**
   * Computes the distance between two given multi-represented objects according to this
   * node's operator (if the node is a inner node) or distance function (if the node is a leaf node).
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given multi-represented objects
   */
  abstract public D distance(M o1, M o2);
}
