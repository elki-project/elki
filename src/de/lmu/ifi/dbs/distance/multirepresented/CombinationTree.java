package de.lmu.ifi.dbs.distance.multirepresented;

import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.AbstractDistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A combination tree is a distance function for multi-represented objects fulfilling two conditions:
 * The leafs represent the different representations and the inner nodes represent the union
 * or intersection operator.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CombinationTree<M extends MetricalObject<M>, O extends MultiRepresentedObject<M>, D extends Distance<D>> extends AbstractDistanceFunction<O, D> {

  /**
   * The root node of this combination tree.
   */
  private final CombinationTreeNode<M, O, D> root;

  private final DistanceFunction<M,D> distanceFunction;

  /**
   * Provides a combination tree with the specified root node.
   * Note that at least one leaf (i.e. one representation) must be
   * added to the root or its children.
   *
   * @param root the root node of this combination tree
   */
  public CombinationTree(CombinationTreeNode<M, O, D> root) {
    super(Pattern.compile(root.getFirstRepresentation().getDistanceFunction().requiredInputPattern()));
    this.root = root;
    this.distanceFunction = root.getFirstRepresentation().getDistanceFunction();
  }

  /**
   * Provides a distance suitable to this DistanceFunction based on the given
   * pattern.
   *
   * @param pattern A pattern defining a distance suitable to this
   *                DistanceFunction
   * @return a distance suitable to this DistanceFunction based on the given
   *         pattern
   * @throws IllegalArgumentException if the given pattern is not compatible with the requirements
   *                                  of this DistanceFunction
   */
  public D valueOf(String pattern) throws IllegalArgumentException {
    return distanceFunction.valueOf(pattern);
  }

  /**
   * Provides an infinite distance.
   *
   * @return an infinite distance
   */
  public D infiniteDistance() {
    return distanceFunction.infiniteDistance();
  }

  /**
   * Provides a null distance.
   *
   * @return a null distance
   */
  public D nullDistance() {
    return distanceFunction.nullDistance();
  }

  /**
   * Provides an undefined distance.
   *
   * @return an undefined distance
   */
  public D undefinedDistance() {
    return distanceFunction.undefinedDistance();
  }

  /**
   * Computes the distance between two given MetricalObjects according to this
   * distance function.
   *
   * @param o1 first MetricalObject
   * @param o2 second MetricalObject
   * @return the distance between two given MetricalObjects according to this
   *         distance function
   */
  public D distance(O o1, O o2) {
    return root.distance(o1, o2);
  }

  /**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    return "Distance function for multirepresented objects. " +
           "No parameters required. " +
           "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
  }

  /**
   * Returns the setting of the attributes of the parameterizable.
   *
   * @return the setting of the attributes of the parameterizable
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    result.addAll(distanceFunction.getAttributeSettings());
    return result;
  }

}
