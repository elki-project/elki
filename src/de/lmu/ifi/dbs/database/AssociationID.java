package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.KernelMatrix;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.tree.interval.IntervalTree;
import de.lmu.ifi.dbs.utilities.ConstantObject;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * An AssociationID is used by databases as a unique identifier for specific
 * associations to single objects. Such as label, local similarity measure.
 * There is no association possible without a specific AssociationID defined
 * within this class. <p/> An AssociationID provides also information concerning
 * the class of the associated objects.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AssociationID extends ConstantObject {
  /**
   * The standard association id to associate a label to an object.
   */
  public static final AssociationID LABEL = new AssociationID("label", String.class);

  /**
   * The association id to associate a class to an object.
   */
  public static final AssociationID CLASS = new AssociationID("class", ClassLabel.class);

  /**
   * The association id to associate an external id to an object.
   */
  public static final AssociationID EXTERNAL_ID = new AssociationID("externalID", String.class);

  /**
   * The association id to associate a correlation pca to an object.
   */
  public static final AssociationID LOCAL_PCA = new AssociationID("pca", LocalPCA.class);

  /**
   * The association id to associate a local dimensionality (e.g. the correlation dimensionality)
   * to an object.
   */
  public static final AssociationID LOCAL_DIMENSIONALITY = new AssociationID("localDimensionality", Integer.class);


  /**
   * The association id to associate the neighbors of an object.
   */
  public static final AssociationID NEIGHBORS = new AssociationID("neighbors", List.class);

  /**
   * The association id to associate the LRD of an object for the LOF
   * algorithm.
   */
  public static final AssociationID LRD = new AssociationID("lrd", Double.class);

  /**
   * The association id to associate the locally weighted matrix of an object
   * for the locally weighted distance function.
   */
  public static final AssociationID LOCALLY_WEIGHTED_MATRIX = new AssociationID("locallyWeightedMatrix", Matrix.class);

  /**
   * The association id to associate a preference vector.
   */
  public static final AssociationID PREFERENCE_VECTOR = new AssociationID("preferenceVector", BitSet.class);

  /**
   * The association id to associate precomputed distances.
   */
  public static final AssociationID CACHED_DISTANCES = new AssociationID("cachedDistances", Map.class);

  /**
   * The association id to associate an interval tree.
   */
  public static final AssociationID INTERVAL_TREE = new AssociationID("intervalTree", IntervalTree.class);

  /**
   * The association id to associate the strong eigencvector weighted matrix of an object.
   */
  public static final AssociationID STRONG_EIGENVECTOR_MATRIX = new AssociationID("strongEigenvectorMatrix", Matrix.class);

  /**
   * The association id to associate an arbitrary matrix of an object.
   */
  public static final AssociationID CACHED_MATRIX = new AssociationID("cachedMatrix", Matrix.class);

  /**
   * The association id to associate a kernel matrix.
   */
  public static final AssociationID KERNEL_MATRIX = new AssociationID("kernelMatrix", KernelMatrix.class);

  /**
   * The serial version UID.
   */
  private static final long serialVersionUID = 8115554038339292192L;

  /**
   * The Class type related to this AssociationID.
   */
  private Class type;

  /**
   * Provides a new AssociationID of the given name and type. <p/> All
   * AssociationIDs are unique w.r.t. their name. An AssociationID provides
   * information of which class the associated objects are.
   *
   * @param name name of the association
   * @param type class of the objects that are associated under this
   *             AssociationID
   */
  private AssociationID(final String name, final Class type) {
    super(name);
    try {
      this.type = Class.forName(type.getName());
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Invalid class name \""
                                         + type.getName() + "\" for property \"" + name + "\".");
    }
  }

  /**
   * Returns the type of the AssociationID.
   *
   * @return the type of the AssociationID
   */
  public Class getType() {
    try {
      return Class.forName(type.getName());
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException("Invalid class name \""
                                      + type.getName() + "\" for property \"" + this.getName()
                                      + "\".");
    }
  }

  /**
   * Returns the AssociationID for the given name if it exists, null
   * otherwise.
   *
   * @param name the name of the desired AssociationID
   * @return the AssociationID for the given name if it exists, null otherwise
   */
  public AssociationID getAssociationID(final String name) {
    return (AssociationID) AssociationID.lookup(AssociationID.class, name);
  }

}
