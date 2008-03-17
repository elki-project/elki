package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.utilities.ConstantObject;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * An AssociationID is used by databases as a unique identifier for specific
 * associations to single objects. Such as label, local similarity measure.
 * There is no association possible without a specific AssociationID defined
 * within this class. <p/> An AssociationID provides also information concerning
 * the class of the associated objects.
 * 
 * @param <C> the type of the class of the associated object
 *
 * @author Arthur Zimek
 */
public class AssociationID<C> extends ConstantObject {
  /**
   * The standard association id to associate a label to an object.
   */
  public static final AssociationID<String> LABEL = new AssociationID<String>("label", String.class);

  /**
   * The association id to associate a class (class label) to an object.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<ClassLabel> CLASS = new AssociationID<ClassLabel>("class", ClassLabel.class);

  /**
   * The association id to associate an external id to an object.
   */
  public static final AssociationID<String> EXTERNAL_ID = new AssociationID<String>("externalID", String.class);

  /**
   * The association id to associate a row id to an object.
   */
  public static final AssociationID<Integer> ROW_ID = new AssociationID<Integer>("rowID", Integer.class);
  
  /**
   * The association id to associate a correlation pca to an object.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<LocalPCA> LOCAL_PCA = new AssociationID<LocalPCA>("pca", LocalPCA.class);

  /**
   * The association id to associate a local dimensionality (e.g. the correlation dimensionality)
   * to an object.
   */
  public static final AssociationID<Integer> LOCAL_DIMENSIONALITY = new AssociationID<Integer>("localDimensionality", Integer.class);


  /**
   * The association id to associate the neighbors of an object.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<List> NEIGHBORS = new AssociationID<List>("neighbors", List.class);

  /**
   * The association id to associate another set of neighbors of an object.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<List> NEIGHBORS_2 = new AssociationID<List>("neighbors2", List.class);
  
  /**
   * The association id to associate a set of neighbors for use of the shared nearest neighbor similarity function.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<SortedSet> SHARED_NEAREST_NEIGHBORS_SET = new AssociationID<SortedSet>("sharedNearestNeighborList", SortedSet.class);

  /**
   * The association id to associate a DoubleDistance to an object.
   */
  public static final AssociationID<DoubleDistance> DOUBLE_DISTANCE = new AssociationID<DoubleDistance>("doubleDistance", DoubleDistance.class);
  
  /**
   * The association id to associate the LRD of an object for the LOF
   * algorithm.
   */
  public static final AssociationID<Double> LRD = new AssociationID<Double>("lrd", Double.class);

  /**
   * The association id to associate the LOF of an object for the LOF
   * algorithm.
   */
  public static final AssociationID<Double> LOF = new AssociationID<Double>("lof", Double.class);
  
  /**
   * AssociationID to associate the probabilities for an instance given a (set of) distribution(s).
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<List> PROBABILITY_X_GIVEN_CLUSTER_I = new AssociationID<List>("P(x|C_i)", List.class);

  /**
   * AssociationID to associate the prior probability for an instance.
   */
  public static final AssociationID<Double> PROBABILITY_X = new AssociationID<Double>("P(x)", Double.class);

  /**
   * AssociationID to associate the probabilities for the clusters for a single instance.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<List> PROBABILITY_CLUSTER_I_GIVEN_X = new AssociationID<List>("P(C_i|x)", List.class);

  
  /**
   * The association id to associate the locally weighted matrix of an object
   * for the locally weighted distance function.
   */
  public static final AssociationID<Matrix> LOCALLY_WEIGHTED_MATRIX = new AssociationID<Matrix>("locallyWeightedMatrix", Matrix.class);

  /**
   * The association id to associate a preference vector.
   */
  public static final AssociationID<BitSet> PREFERENCE_VECTOR = new AssociationID<BitSet>("preferenceVector", BitSet.class);

  /**
   * The association id to associate precomputed distances.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<Map> CACHED_DISTANCES = new AssociationID<Map>("cachedDistances", Map.class);

  /**
   * The association id to associate the strong eigencvector weighted matrix of an object.
   */
  public static final AssociationID<Matrix> STRONG_EIGENVECTOR_MATRIX = new AssociationID<Matrix>("strongEigenvectorMatrix", Matrix.class);

  /**
   * The association id to associate an arbitrary matrix of an object.
   */
  public static final AssociationID<Matrix> CACHED_MATRIX = new AssociationID<Matrix>("cachedMatrix", Matrix.class);

  /**
   * The association id to associate a kernel matrix.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<KernelMatrix> KERNEL_MATRIX = new AssociationID<KernelMatrix>("kernelMatrix", KernelMatrix.class);

  /**
   * The association id to associate any arbitrary object.
   */
  public static final AssociationID<Object> OBJECT = new AssociationID<Object>("object", Object.class);

  /**
   * The association id to associate a fractal dimension cluster.
   */
  @SuppressWarnings("unchecked")
public static final AssociationID<HierarchicalFractalDimensionCluster> FRACTAL_DIMENSION_CLUSTER = new AssociationID<HierarchicalFractalDimensionCluster>("fractalDimensionCluster", HierarchicalFractalDimensionCluster.class);

  /**
   * The serial version UID.
   */
  private static final long serialVersionUID = 8115554038339292192L;

  /**
   * The Class type related to this AssociationID.
   */
  private Class<C> type;

  /**
   * Provides a new AssociationID of the given name and type. <p/> All
   * AssociationIDs are unique w.r.t. their name. An AssociationID provides
   * information of which class the associated objects are.
   *
   * @param name name of the association
   * @param type class of the objects that are associated under this
   *             AssociationID
   */
  @SuppressWarnings("unchecked")
private AssociationID(final String name, final Class<C> type) {
    super(name);
    try {
      this.type = (Class<C>) Class.forName(type.getName());
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
  @SuppressWarnings("unchecked")
public Class<C> getType() {
    try {
      return (Class<C>) Class.forName(type.getName());
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
  public AssociationID<?> getAssociationID(final String name) {
    return (AssociationID) AssociationID.lookup(AssociationID.class, name);
  }

}
