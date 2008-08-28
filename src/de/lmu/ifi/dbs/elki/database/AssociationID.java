package de.lmu.ifi.dbs.elki.database;


import de.lmu.ifi.dbs.elki.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.elki.algorithm.result.MultivariateModel;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.ConstantObject;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;

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
 * @author Arthur Zimek
 * @param <C> the type of the class of the associated object
 */
public class AssociationID<C> extends ConstantObject<AssociationID<C>> {
    /**
     * The standard association id to associate a label to an object.
     */
    public static final AssociationID<String> LABEL = new AssociationID<String>("label", String.class);

    /**
     * The association id to associate a class (class label) to an object.
     */
    public static final AssociationID<ClassLabel<?>> CLASS = new AssociationID<ClassLabel<?>>("class", ClassLabel.class);

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
    public static final AssociationID<PCAFilteredResult> LOCAL_PCA = new AssociationID<PCAFilteredResult>("pca", PCAFilteredResult.class);

    /**
     * The association id to associate a local dimensionality (e.g. the
     * correlation dimensionality) to an object.
     */
    public static final AssociationID<Integer> LOCAL_DIMENSIONALITY = new AssociationID<Integer>("localDimensionality", Integer.class);

    /**
     * The association id to associate the neighbors of an object.
     */
    public static final AssociationID<List<Integer>> NEIGHBOR_IDS = new AssociationID<List<Integer>>("neighborids", List.class);

    /**
     * The association id to associate the neighbors of an object.
     */
    public static final AssociationID<List<QueryResult<DoubleDistance>>> NEIGHBORS = new AssociationID<List<QueryResult<DoubleDistance>>>("neighbors", List.class);

    /**
     * The association id to associate another set of neighbors of an object.
     */
    public static final AssociationID<List<QueryResult<DoubleDistance>>> NEIGHBORS_2 = new AssociationID<List<QueryResult<DoubleDistance>>>("neighbors2", List.class);

    /**
     * The association id to associate a set of neighbors for use of the shared
     * nearest neighbor similarity function.
     */
    public static final AssociationID<SortedSet<?>> SHARED_NEAREST_NEIGHBORS_SET = new AssociationID<SortedSet<?>>("sharedNearestNeighborList", SortedSet.class);

    /**
     * The association id to associate a DoubleDistance to an object.
     */
    public static final AssociationID<DoubleDistance> DOUBLE_DISTANCE = new AssociationID<DoubleDistance>("doubleDistance", DoubleDistance.class);

    /**
     * The association id to associate the LRD of an object for the LOF algorithm.
     */
    public static final AssociationID<Double> LRD = new AssociationID<Double>("lrd", Double.class);

    /**
     * The association id to associate the LOF of an object for the LOF algorithm.
     */
    public static final AssociationID<Double> LOF = new AssociationID<Double>("lof", Double.class);

    /**
   * The association id to associate the Correlation Outlier Probability of an object
   */
  public static final AssociationID<Double> COP = new AssociationID<Double>("cop", Double.class);
  
  /**
   * The association id to associate the COP error vector of an object for the COP
   * algorithm.
   */
  public static final AssociationID<Vector> COP_ERROR_VECTOR = new AssociationID<Vector>("cop error vector", Vector.class);
  
  /**
   * The association id to associate the COP data vector of an object for the COP
   * algorithm.
   */
  public static final AssociationID<Matrix> COP_DATA_VECTORS = new AssociationID<Matrix>("cop data vectors", Matrix.class);
  
  /**
   * The association id to associate the COP correlation dimensionality of an object for the COP
   * algorithm.
   */
  public static final AssociationID<Integer> COP_DIM = new AssociationID<Integer>("cop dim", Integer.class);
  
  /**
   * The association id to associate the COP correlation solution
   */
  public static final AssociationID<CorrelationAnalysisSolution<?>> COP_SOL = new AssociationID<CorrelationAnalysisSolution<?>>("cop sol", CorrelationAnalysisSolution.class);
  
  /**
   * The association id to associate the Multivariate Correlation Outlier Probability of an object
   */
  public static final AssociationID<MultivariateModel<?>> LOCAL_MODEL = new AssociationID<MultivariateModel<?>>("multivariate model", MultivariateModel.class);
  
  /**
   * The association id to associate the Multivariate Correlation Outlier Probability of an object
   */
  public static final AssociationID<Double> MCOP = new AssociationID<Double>("mcop", Double.class);
  
  /**
     * AssociationID to associate the probabilities for an instance given a (set
     * of) distribution(s).
     */
    @SuppressWarnings("unchecked")
    public static final AssociationID<List> PROBABILITY_X_GIVEN_CLUSTER_I = new AssociationID<List>("P(x|C_i)", List.class);

    /**
     * AssociationID to associate the prior probability for an instance.
     */
    public static final AssociationID<Double> PROBABILITY_X = new AssociationID<Double>("P(x)", Double.class);

    /**
     * AssociationID to associate the probabilities for the clusters for a single
     * instance.
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
     * The association id to associate the strong eigencvector weighted matrix of
     * an object.
     */
    public static final AssociationID<Matrix> STRONG_EIGENVECTOR_MATRIX = new AssociationID<Matrix>("strongEigenvectorMatrix", Matrix.class);

    /**
     * The association id to associate an arbitrary matrix of an object.
     */
    public static final AssociationID<Matrix> CACHED_MATRIX = new AssociationID<Matrix>("cachedMatrix", Matrix.class);

    /**
     * The association id to associate a kernel matrix.
     */
    public static final AssociationID<KernelMatrix<?>> KERNEL_MATRIX = new AssociationID<KernelMatrix<?>>("kernelMatrix", KernelMatrix.class);

    /**
     * The association id to associate any arbitrary object.
     */
    public static final AssociationID<Object> OBJECT = new AssociationID<Object>("object", Object.class);



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
    private AssociationID(final String name, final Class<?> type) {
        // It's more useful to use Class<?> here to allow the use of nested
        // Generics such as List<Foo<Bar>>
        super(name);
        try {
            this.type = (Class<C>) Class.forName(type.getName());
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid class name \"" + type.getName() + "\" for property \"" + name + "\".");
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
            throw new IllegalStateException("Invalid class name \"" + type.getName() + "\" for property \"" + this.getName() + "\".");
        }
    }

    /**
     * Returns the AssociationID for the given name if it exists, null otherwise.
     *
     * @param name the name of the desired AssociationID
     * @return the AssociationID for the given name if it exists, null otherwise
     */
    @SuppressWarnings("unchecked")
    public static AssociationID<?> getAssociationID(final String name) {
        return AssociationID.lookup(AssociationID.class, name);
    }

    /**
     * Gets or creates the AssociationID for the given name and given type.
     *
     * @param name the name
     * @param type the type of the association
     * @return the AssociationID for the given name
     */
    @SuppressWarnings("unchecked")
    public static <C> AssociationID<C> getOrCreateAssociationID(final String name, final Class<C> type) {
        AssociationID<C> associationID = (AssociationID<C>) getAssociationID(name);
        if (associationID == null) {
            associationID = new AssociationID<C>(name, type);
        }
        return associationID;
    }

}
