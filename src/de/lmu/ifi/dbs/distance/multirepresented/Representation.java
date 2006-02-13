package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

/**
 * Represents a leaf node in a combination tree, i.e. represents one representation
 * of a multi-represented object. The distance between two mult-represented objcets is computed as the
 * distance between the two representations of these objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Representation<O extends DatabaseObject<O>, M extends MultiRepresentedObject<O>, D extends Distance<D>>
extends CombinationTreeNode<O, M, D> {

  /**
   * The distance function for this representation.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * The index of this representation in a multi-represented object.
   */
  private Integer representationIndex;

  public Representation(DistanceFunction<O, D> distanceFunction, Integer representationIndex) {
    this.distanceFunction = distanceFunction;
    this.representationIndex = representationIndex;
  }

  /**
   * Returns this representation.
   *
   * @return this
   */
  Representation<O,M,D> getFirstRepresentation() {
    return this;
  }

  /**
   * Returns the distance function of this representation.
   *
   * @return the distance function of this representation
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * Computes the distance between two given multi-represented objects according to this
   * node's operator (if the node is a inner node) or distance function (if the node is a leaf node).
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given multi-represented objects
   */
  public D distance(M o1, M o2) {
    return distanceFunction.distance(o1.getRepresentation(representationIndex),
                                     o2.getRepresentation(representationIndex));
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  public String toString() {
    return CombinationTree.REPRESENTATION_PREFIX +
           CombinationTree.REPRESENTATION_SEPARATOR +
           representationIndex +
           CombinationTree.REPRESENTATION_SEPARATOR +
           distanceFunction.getClass().getName();
  }


}
