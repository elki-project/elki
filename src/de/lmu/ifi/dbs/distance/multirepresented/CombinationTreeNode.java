package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.Distance;

/**
 * Represents a node in a combination tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CombinationTreeNode<O extends MultiRepresentedObject, D extends Distance<D>> {

  /**
   * Returns a String as description of the required input format.
   *
   * @return a String as description of the required input format
   */
  abstract String requiredInputPattern();

  /**
   * Provides a distance based on the given pattern.
   *
   * @param pattern A pattern defining a distance suitable to this node
   * @return a distance suitable to this node based on the given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this node
   */
  abstract public D valueOf(String pattern) throws IllegalArgumentException;

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  abstract D infiniteDistance();

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  abstract D nullDistance();

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  abstract D undefinedDistance();

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
