package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The reachability distance as used by LOF.
 * 
 * Reachability of A <em>from</em> B, i.e.
 * 
 * <pre>
 *   reachability-distance(A,B) = max( k-distance(B), distance(A,B) )
 * </pre>
 * 
 * Where <tt>k-distance(B)</tt> is the distance to the k nearest neighbor of B,
 * and <tt>distance</tt> is the actual distance of A and B.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url = "http://dx.doi.org/10.1145/342009.335388")
public class ReachabilityDistance<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceFunction<O, D> {
  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("reachdist.basedistance", "Base distance function to use.");

  /**
   * OptionID for {@link #KNNQUERY_PARAM}
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("reachdist.knnquery", "kNN query to use");

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("reachdist.k", "The number of nearest neighbors of an object to be considered for computing its reachability distance.");

  /**
   * KNN query to use.
   */
  protected KNNQuery<O, D> knnQuery;

  /**
   * The distance function to determine the exact distance.
   */
  protected DistanceFunction<O, D> distanceFunction;

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
  public ReachabilityDistance(DistanceFunction<O, D> distanceFunction, KNNQuery<O, D> knnQuery) {
    super(distanceFunction.getDistanceFactory());
    this.distanceFunction = distanceFunction;
    this.knnQuery = knnQuery;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param <O> Object type
   * @param config Parameterization
   * @return Distance function
   */
  public static <O extends DatabaseObject, D extends Distance<D>> ReachabilityDistance<O, D> parameterize(Parameterization config) {
    // parameter distance function
    final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
    DistanceFunction<O, D> distanceFunction = null;
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
    // parameter k
    final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));
    int k = 2;
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    // configure first preprocessor
    final ClassParameter<KNNQuery<O, D>> KNNQUERY_PARAM = new ClassParameter<KNNQuery<O, D>>(KNNQUERY_ID, KNNQuery.class, PreprocessorKNNQuery.class);
    KNNQuery<O, D> knnQuery = null;
    if(config.grab(KNNQUERY_PARAM) && distanceFunction != null) {
      ListParameterization query1Params = new ListParameterization();
      query1Params.addParameter(KNNQuery.K_ID, k + (objectIsInKNN ? 0 : 1));
      query1Params.addParameter(KNNQuery.DISTANCE_FUNCTION_ID, distanceFunction);
      ChainedParameterization chain = new ChainedParameterization(query1Params, config);
      // chain.errorsTo(config);
      knnQuery = KNNQUERY_PARAM.instantiateClass(chain);
      query1Params.reportInternalParameterizationErrors(config);
    }
    if(distanceFunction != null && knnQuery != null) {
      return new ReachabilityDistance<O, D>(distanceFunction, knnQuery);
    }
    return null;
  }

  @Override
  public D distance(O o1, O o2) {
    if(o2.getID() == null) {
      throw new UnsupportedOperationException();
    }
    D truedist = distanceFunction.distance(o1, o2);
    List<DistanceResultPair<D>> neighborhood = knnQuery.get(o2.getID());
    return computeReachdist(neighborhood, truedist);
  }

  @Override
  public D distance(DBID id1, DBID id2) {
    List<DistanceResultPair<D>> neighborhood = knnQuery.get(id2);
    D truedist = distanceFunction.distance(id1, id2);
    return computeReachdist(neighborhood, truedist);
  }

  @Override
  public D distance(O o1, DBID id2) {
    List<DistanceResultPair<D>> neighborhood = knnQuery.get(id2);
    D truedist = distanceFunction.distance(o1, id2);
    return computeReachdist(neighborhood, truedist);
  }

  /**
   * Actually compute the distance, whichever way we obtained the neighborhood
   * above.
   * 
   * @param neighborhood Neighborhood
   * @param truedist True distance
   * @return Reachability distance
   */
  private D computeReachdist(List<DistanceResultPair<D>> neighborhood, D truedist) {
    // TODO: need to check neighborhood size?
    // TODO: Do we need to check we actually got the object itself in the
    // neighborhood?
    D kdist = neighborhood.get(neighborhood.size() - 1).first;
    return DistanceUtil.max(kdist, truedist);
  }

  @Override
  public Class<? super O> getInputDatatype() {
    return knnQuery.getInputDatatype();
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
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    knnQuery.setDatabase(database);
  }
}