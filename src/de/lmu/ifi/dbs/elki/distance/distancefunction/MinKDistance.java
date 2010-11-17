package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQueryFactory;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQueryFactory;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A distance that is at least the distance to the kth nearest neighbor.
 * 
 * This is essentially the "reachability distance" of LOF, but with arguments
 * reversed!
 * 
 * Reachability of B <em>from</em> A, i.e.
 * 
 * <pre>
 *   reachability-distance(A,B) = max( k-distance(A), distance(A,B) )
 * </pre>
 * 
 * Where <tt>k-distance(A)</tt> is the distance to the k nearest neighbor of A,
 * and <tt>distance</tt> is the actual distance of A and B.
 * 
 * This distance is NOT symmetric. You need to pay attention to the order of
 * arguments!
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class MinKDistance<O extends DatabaseObject, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> {
  /**
   * OptionID for the base distance used to compute reachability
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("reachdist.basedistance", "Base distance function to use.");

  /**
   * OptionID for the KNN query class to use (preprocessor, approximation, ...)
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("reachdist.knnquery", "kNN query to use");

  /**
   * OptionID for the "k" parameter.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("reachdist.k", "The number of nearest neighbors of an object to be considered for computing its reachability distance.");

  /**
   * KNN query to use.
   */
  protected KNNQueryFactory<O, D> knnQuery;

  /**
   * The distance function to determine the exact distance.
   */
  protected DistanceFunction<? super O, D> parentDistance;

  /**
   * The value of k
   */
  private int k;

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  static boolean objectIsInKNN = false;

  /**
   * Full constructor. See {@link #parameterize} for factory.
   * 
   * @param knnQuery query to use
   */
  public MinKDistance(KNNQueryFactory<O, D> knnQuery, int k) {
    super();
    this.parentDistance = knnQuery.getDistanceFunction();
    this.k = k;
    this.knnQuery = knnQuery;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param <O> Object type
   * @param config Parameterization
   * @return Distance function
   */
  public static <O extends DatabaseObject, D extends Distance<D>> MinKDistance<O, D> parameterize(Parameterization config) {
    // parameter k
    final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));
    int k = 2;
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    // configure first preprocessor
    final ClassParameter<KNNQueryFactory<O, D>> KNNQUERY_PARAM = new ClassParameter<KNNQueryFactory<O, D>>(KNNQUERY_ID, KNNQueryFactory.class, PreprocessorKNNQueryFactory.class);
    KNNQueryFactory<O, D> knnQuery = null;
    if(config.grab(KNNQUERY_PARAM)) {
      ListParameterization query1Params = new ListParameterization();
      query1Params.addParameter(KNNQueryFactory.K_ID, k + (objectIsInKNN ? 0 : 1));
      ChainedParameterization chain = new ChainedParameterization(query1Params, config);
      // chain.errorsTo(config);
      knnQuery = KNNQUERY_PARAM.instantiateClass(chain);
      query1Params.reportInternalParameterizationErrors(config);
    }
    if(knnQuery != null) {
      return new MinKDistance<O, D>(knnQuery, k);
    }
    return null;
  }

  @Override
  public <T extends O> DistanceQuery<T, D> instantiate(Database<T> database) {
    return new Instance<T>(database, k);
  }

  /**
   * Instance for an actual database.
   * 
   * @author Erich Schubert
   */
  public class Instance<T extends O> extends AbstractDBIDDistanceQuery<T, D> {
    /**
     * KNN query instance
     */
    private KNNQuery<T, D> knnQueryInstance;
    
    /**
     * Value for k
     */
    private int k;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param k Value of k
     */
    public Instance(Database<T> database, int k) {
      super(database);
      this.k = k;
      this.knnQueryInstance = knnQuery.instantiate(database);
    }

    @Override
    public D distance(DBID id1, DBID id2) {
      List<DistanceResultPair<D>> neighborhood = knnQueryInstance.getKNNForDBID(id1, k);
      D truedist = knnQueryInstance.getDistanceQuery().distance(id1, id2);
      return computeReachdist(neighborhood, truedist);
    }

    @Override
    public DistanceFunction<? super T, D> getDistanceFunction() {
      return MinKDistance.this;
    }
  }

  /**
   * Actually compute the distance, whichever way we obtained the neighborhood
   * above.
   * 
   * @param neighborhood Neighborhood
   * @param truedist True distance
   * @return Reachability distance
   */
  protected D computeReachdist(List<DistanceResultPair<D>> neighborhood, D truedist) {
    // TODO: need to check neighborhood size?
    // TODO: Do we need to check we actually got the object itself in the
    // neighborhood?
    D kdist = neighborhood.get(neighborhood.size() - 1).first;
    return DistanceUtil.max(kdist, truedist);
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return false;
  }

  @Override
  public D getDistanceFactory() {
    return knnQuery.getDistanceFactory();
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return DatabaseObject.class;
  }
}