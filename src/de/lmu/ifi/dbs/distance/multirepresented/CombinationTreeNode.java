package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;

/**
 * Represents a node in a combination tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CombinationTreeNode<M extends MetricalObject<M>, O extends MultiRepresentedObject<M>, D extends Distance<D>> {

  /**
   * Returns the first representation child of this node that would be visited during
   * a depth first enumeration.
   *
   * @return the first representation child of this node that would be visited during
   * a depth first enumeration
   */
  abstract Representation<M,O,D> getFirstRepresentation();

  /**
   * Computes the distance between two given multi-represented objects according to this
   * node's operator (if the node is a inner node) or distance function (if the node is a leaf node).
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given multi-represented objects
   */
  abstract public D distance(O o1, O o2);
}
