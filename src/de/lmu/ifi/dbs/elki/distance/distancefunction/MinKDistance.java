package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
   * @param parentDistance distance function to use
   * @param k K parameter
   */
  public MinKDistance(DistanceFunction<? super O, D> parentDistance, int k) {
    super();
    this.parentDistance = parentDistance;
    this.k = k;
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
    final ObjectParameter<DistanceFunction<? super O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
    DistanceFunction<? super O, D> distanceFunction = null;
    if (config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
    return new MinKDistance<O, D>(distanceFunction, k + (objectIsInKNN ? 0 : 1));
  }

  @Override
  public <T extends O> DistanceQuery<T, D> instantiate(Database<T> database) {
    return new Instance<T>(database, k, parentDistance);
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
    private KNNQuery<T, D> knnQuery;
    
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
    public Instance(Database<T> database, int k, DistanceFunction<? super O, D> parentDistance) {
      super(database);
      this.k = k;
      this.knnQuery= database.getKNNQuery(parentDistance, k, DatabaseQuery.HINT_HEAVY_USE);
    }

    @Override
    public D distance(DBID id1, DBID id2) {
      List<DistanceResultPair<D>> neighborhood = knnQuery.getKNNForDBID(id1, k);
      D truedist = knnQuery.getDistanceQuery().distance(id1, id2);
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
    return parentDistance.getDistanceFactory();
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return DatabaseObject.class;
  }
}