package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;

/**
 * Represents a leaf node in a combination tree, i.e. represents one representation
 * of a multi-represented object. The distance between two mult-represented objcets is computed as the
 * distance between the two representations of these objects.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Representation<M extends MetricalObject<M>, O extends MultiRepresentedObject<M>, D extends Distance<D>>
extends CombinationTreeNode<O,D> {

  /**
   * The distance function for this representation.
   */
  private DistanceFunction<M,D> distanceFunction;

  /**
   * The index of this representation in a multi-represented object.
   */
  private Integer representationIndex;

  public Representation(DistanceFunction<M, D> distanceFunction, Integer representationIndex) {
    this.distanceFunction = distanceFunction;
    this.representationIndex = representationIndex;
  }

  /**
   * Returns a String as description of the required input format.
   *
   * @return a String as description of the required input format
   */
  String requiredInputPattern() {
    return distanceFunction.requiredInputPattern();
  }

  /**
   * Provides a distance based on the given pattern.
   *
   * @param pattern A pattern defining a distance suitable to this representation's distance function
   * @return a distance suitable to this node based on the given pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this representation's distance function
   */
  public D valueOf(String pattern) throws IllegalArgumentException {
    return distanceFunction.valueOf(pattern);
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  D infiniteDistance() {
    return distanceFunction.infiniteDistance();
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  D nullDistance() {
    return distanceFunction.nullDistance();
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  D undefinedDistance() {
    return distanceFunction.undefinedDistance();
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
    return distanceFunction.distance(o1.getRepresentation(representationIndex),
                                     o2.getRepresentation(representationIndex));
  }


}
