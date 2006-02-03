package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.Distance;

/**
 * Represents a inner node in a combination tree, i.e. represents the union or intersection
 * operator.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Operator<M extends MetricalObject<M>, O extends MultiRepresentedObject<M>, D extends Distance<D>>
extends CombinationTreeNode<M, O, D> {
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

  /**
   * The left child of this node.
   */
  private CombinationTreeNode<M, O, D> leftChild;

  /**
   * The right child of this node.
   */
  private CombinationTreeNode<M, O, D> rightChild;

  /**
   * Creates a new operator node in a combination tree representing the specified
   * operator type with no children.
   *
   * @param type
   */
  public Operator(int type) {
    if (type == UNION || type == INTERSECTION) this.type = type;
    else throw new IllegalArgumentException("Illegeal type specified!");
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
   * Adds the specified <code>child</code> as this node's left child.
   *
   * @param child node to add
   */
  public void addLeftChild(CombinationTreeNode<M, O, D> child) {
    this.leftChild = child;
  }

  /**
   * Adds the specified <code>child</code> as this node's right child.
   *
   * @param child node to add
   */
  public void addRightChild(CombinationTreeNode<M, O, D> child) {
    this.rightChild = child;
  }

  /**
   * Computes the minimum distance between two given multi-represented objects.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the minimum distance between two given multi-represented objects
   */
  private D unionDistance(O o1, O o2) {
    D d1 = leftChild.distance(o1, o2);
    D d2 = rightChild.distance(o1, o2);

    if (d1.compareTo(d2) <= 0) return d1;
    return d2;
  }

  /**
   * Computes the maximum distance between two given multi-represented objects.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the maximum distance between two given multi-represented objects
   */
  private D intersectionDistance(O o1, O o2) {
    D d1 = leftChild.distance(o1, o2);
    D d2 = rightChild.distance(o1, o2);

    if (d1.compareTo(d2) >= 0) return d1;
    return d2;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    String result = "";
    if (type == UNION) {
      result += CombinationTree.UNION_OPERATOR;

    }
    else {
      result += CombinationTree.INTERSECTION_OPERATOR;
    }
    result += " " + leftChild.toString();
    result += " " + rightChild.toString();
    return result;
  }
}
