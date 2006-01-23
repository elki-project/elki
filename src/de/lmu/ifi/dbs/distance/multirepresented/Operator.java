package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.Distance;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a inner node in a combination tree, i.e. represents the union or intersection
 * operator.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Operator<O extends MultiRepresentedObject, D extends Distance<D>> extends CombinationTreeNode<O, D> {
  /**
   * The UNION operator type.
   */
  public static int UNION = 1;

  /**
   * The INTERSECTION operator type.
   */
  public static int INTERSECTION = 2;

  /**
   * The operator type of this node.
   */
  private int type;

  private List<CombinationTreeNode<O, D>> children;

  /**
   * Creates a new operator node in a combination tree representing the specified
   * operator type with no children.
   *
   * @param type
   */
  public Operator(int type) {
    if (type == UNION || type == INTERSECTION) this.type = type;
    else throw new IllegalArgumentException("Illegeal type specified!");

    this.children = new ArrayList<CombinationTreeNode<O, D>>();
  }

  /**
   * Returns a String as description of the required input format.
   * The required input format is determined from the first children of this node.
   *
   * @return a String as description of the required input format
   */
  String requiredInputPattern() {
    if (children.isEmpty()) throw new IllegalStateException("Node has no children!");

    return children.get(0).requiredInputPattern();
  }

  /**
   * Provides a distance based on the given pattern.
   * The distance is determined from the first children of this node.
   *
   * @param pattern A pattern defining a distance suitable to this node
   * @return a distance suitable to this node based on the given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this node
   */
  public D valueOf(String pattern) throws IllegalArgumentException {
    if (children.isEmpty()) throw new IllegalStateException("Node has no children!");

    return children.get(0).valueOf(pattern);
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  D infiniteDistance() {
    if (children.isEmpty()) throw new IllegalStateException("Node has no children!");
    return children.get(0).infiniteDistance();
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  D nullDistance() {
    if (children.isEmpty()) throw new IllegalStateException("Node has no children!");
    return children.get(0).nullDistance();
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  D undefinedDistance() {
    if (children.isEmpty()) throw new IllegalStateException("Node has no children!");
    return children.get(0).undefinedDistance();
  }

  /**
   * Computes the distance between two given multi-represented objects according to this
   * node's operator (if the node is a inner node) or distance function (if the node is a leaf node).
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given multi-represented objects
   */
  public D distance(O o1, O o2) {
    if (type == UNION) {
      return unionDistance(o1, o2);
    }
    else if (type == INTERSECTION) {
      return intersectionDistance(o1, o2);
    }
    else throw new RuntimeException("Should never happen!");
  }

  /**
   * Adds the specified <code>child</code> to this node's child array.
   *
   * @param child node to add as a child of this node
   */
  public void add(CombinationTreeNode<O, D> child) {
    if (child != null)
      children.add(child);
  }

  /**
   * Computes the union distance between two given multi-represented objects.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the union distance between two given multi-represented objects
   */
  private D unionDistance(O o1, O o2) {
    D min = null;
    for (CombinationTreeNode<O, D> child : children) {
      D distance = child.distance(o1, o2);
      if (min == null || distance.compareTo(min) < 0) {
        min = distance;
      }
    }
    return min;
  }

  /**
   * Computes the intersection distance between two given multi-represented objects.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the intersection distance between two given multi-represented objects
   */
  private D intersectionDistance(O o1, O o2) {
    D max = null;
    for (CombinationTreeNode<O, D> child : children) {
      D distance = child.distance(o1, o2);
      if (max == null || distance.compareTo(max) > 0) {
        max = distance;
      }
    }
    return max;
  }
}
