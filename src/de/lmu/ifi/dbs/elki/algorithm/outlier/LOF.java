package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.preprocessing.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromDataStore;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter {@link #K_ID} ({@code -lof.k}).
 * </p>
 * 
 * <p>
 * This implementation diverts from the original LOF publication in that it
 * allows the user to use a different distance function for the reachability
 * distance and neighborhood determination (although the default is to use the
 * same value.)
 * </p>
 * 
 * <p>
 * The k nearest neighbors are determined using the parameter
 * {@link AbstractDistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}, while the
 * reference set used in reachability distance computation is configured using
 * {@link #REACHABILITY_DISTANCE_FUNCTION_ID}.
 * </p>
 * 
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;. Since kNN queries
 * in ELKI have slightly different semantics - exactly k neighbors are returned
 * - we chose to rename the parameter to {@link #K_ID} ({@code -lof.k}) to
 * reflect this difference.
 * </p>
 * 
 * <p>
 * Reference: <br>
 * M. M. Breunig, H.-P. Kriegel, R. Ng, J. Sander: LOF: Identifying
 * Density-Based Local Outliers. <br>
 * In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00),
 * Dallas, TX, 2000.
 * </p>
 * 
 * @author Peer Kr&ouml;ger
 * @author Erich Schubert
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url = "http://dx.doi.org/10.1145/342009.335388")
public class LOF<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> {
  /**
   * OptionID for {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("lof.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -lof.reachdistfunction}
   * </p>
   */
  private final ObjectParameter<DistanceFunction<O, D>> REACHABILITY_DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);

  /**
   * The association id to associate the LOF_SCORE of an object for the
   * LOF_SCORE algorithm.
   */
  public static final AssociationID<Double> LOF_SCORE = AssociationID.getOrCreateAssociationID("lof", Double.class);

  /**
   * Holds the instance of the reachability distance function specified by
   * {@link #REACHABILITY_DISTANCE_FUNCTION_PARAM}.
   */
  protected DistanceFunction<O, D> reachabilityDistanceFunction;

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("lof.k", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   * <p>
   * Key: {@code -lof.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * OptionID for {@link #KNNQUERY_PARAM}
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("lof.knnquery", "kNN query to use");

  /**
   * The preprocessor used to materialize the kNN neighborhoods.
   * 
   * Default value: {@link PreprocessorKNNQuery} </p>
   * <p>
   * Key: {@code -lof.knnquery}
   * </p>
   */
  private final ClassParameter<KNNQuery<O, D>> KNNQUERY_PARAM = new ClassParameter<KNNQuery<O, D>>(KNNQUERY_ID, getKNNQueryRestriction(), PreprocessorKNNQuery.class);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k = 2;

  /**
   * Preprocessor Step 1
   */
  protected KNNQuery<O, D> knnQuery1;

  /**
   * Preprocessor Step 2
   */
  protected KNNQuery<O, D> knnQuery2;

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  boolean objectIsInKNN = false;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public LOF(Parameterization config) {
    super(config);
    // parameter k
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    // parameter reachability distance function
    if(config.grab(REACHABILITY_DISTANCE_FUNCTION_PARAM)) {
      reachabilityDistanceFunction = REACHABILITY_DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
    else {
      reachabilityDistanceFunction = getDistanceFunction();
    }

    // configure first preprocessor
    if(config.grab(KNNQUERY_PARAM) && DISTANCE_FUNCTION_PARAM.isDefined()) {
      ListParameterization query1Params = new ListParameterization();
      query1Params.addParameter(KNNQuery.K_ID, k + (objectIsInKNN ? 0 : 1));
      query1Params.addParameter(KNNQuery.DISTANCE_FUNCTION_ID, getDistanceFunction());
      ChainedParameterization chain = new ChainedParameterization(query1Params, config);
      // chain.errorsTo(config);
      knnQuery1 = KNNQUERY_PARAM.instantiateClass(chain);
      query1Params.reportInternalParameterizationErrors(config);

      if(reachabilityDistanceFunction != null && REACHABILITY_DISTANCE_FUNCTION_PARAM.isDefined()) {
        // configure second preprocessor
        ListParameterization query2Params = new ListParameterization();
        query2Params.addParameter(KNNQuery.K_ID, k + (objectIsInKNN ? 0 : 1));
        query2Params.addParameter(KNNQuery.DISTANCE_FUNCTION_ID, reachabilityDistanceFunction);
        ChainedParameterization chain2 = new ChainedParameterization(query2Params, config);
        // chain2.errorsTo(config);
        knnQuery2 = KNNQUERY_PARAM.instantiateClass(chain2);
        query2Params.reportInternalParameterizationErrors(config);
      }
    }
  }

  /**
   * KNN query restriction.
   * 
   * @return KNN query restriction
   */
  protected Class<?> getKNNQueryRestriction() {
    return KNNQuery.class;
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)}.
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    return doRunInTime(database).getResult();
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database and
   * returns a {@link LOFResult} encapsulating information that may be needed by
   * an {@link OnlineLOF} algorithm.
   */
  protected LOFResult doRunInTime(Database<O> database) throws IllegalStateException {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(4) : null;

    // neighborhood queries in use, map to defined queries.
    KNNQuery.Instance<O, D> neigh1;
    KNNQuery.Instance<O, D> neigh2;
    if(stepprog != null) {
      stepprog.beginStep(1, "Materializing Neighborhoods with respect to primary distance.", logger);
    }
    neigh1 = knnQuery1.instantiate(database);
    if(getDistanceFunction() != reachabilityDistanceFunction) {
      if(stepprog != null) {
        stepprog.beginStep(2, "Materializing Neighborhoods with respect to reachability distance.", logger);
      }
      neigh2 = knnQuery2.instantiate(database);
    }
    else {
      if(stepprog != null) {
        stepprog.beginStep(2, "Reusing neighborhoods of primary distance.", logger);
      }
      neigh2 = neigh1;
    }

    // Compute LRDs
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LRDs", logger);
    }
    WritableDataStore<Double> lrds = computeLRDs(database.getIDs(), neigh2);

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(4, "computing LOFs", logger);
    }
    Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(database.getIDs(), lrds, neigh1);
    WritableDataStore<Double> lofs = lofsAndMax.getFirst();
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = lofsAndMax.getSecond();

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>(LOF_SCORE, lofs);
    OrderingResult orderingResult = new OrderingFromDataStore<Double>(lofs, true);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult, orderingResult);

    return new LOFResult(result, neigh1, neigh2, lrds, lofs);
  }

  /**
   * Computes the local reachability density (LRD) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param neigh2 the precomputed neighborhood of the objects w.r.t. the
   *        reachability distance
   * @return the LRDs of the objects
   */
  protected WritableDataStore<Double> computeLRDs(DBIDs ids, KNNQuery.Instance<O, D> neigh2) {
    WritableDataStore<Double> lrds = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    FiniteProgress lrdsProgress = logger.isVerbose() ? new FiniteProgress("LRD", ids.size(), logger) : null;
    int counter = 0;
    for(DBID id : ids) {
      counter++;
      double sum = 0;
      List<DistanceResultPair<D>> neighbors = neigh2.get(id);
      int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
      for(DistanceResultPair<D> neighbor : neighbors) {
        if(objectIsInKNN || neighbor.getID() != id) {
          List<DistanceResultPair<D>> neighborsNeighbors = neigh2.get(neighbor.getID());
          sum += Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().doubleValue());
        }
      }
      Double lrd = (sum > 0) ? nsize / sum : 0.0;
      lrds.put(id, lrd);
      if(lrdsProgress != null) {
        lrdsProgress.setProcessed(counter, logger);
      }
    }
    if(lrdsProgress != null) {
      lrdsProgress.ensureCompleted(logger);
    }
    return lrds;
  }

  /**
   * Computes the Local outlier factor (LOF) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param lrds the LRDs of the objects
   * @param neigh1 the precomputed neighborhood of the objects w.r.t. the
   *        primary distance
   * @return the LOFs of the objects and the maximum LOF
   */
  protected Pair<WritableDataStore<Double>, MinMax<Double>> computeLOFs(DBIDs ids, DataStore<Double> lrds, KNNQuery.Instance<O, D> neigh1) {
    WritableDataStore<Double> lofs = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_STATIC, Double.class);
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = new MinMax<Double>();

    FiniteProgress progressLOFs = logger.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), logger) : null;
    int counter = 0;
    for(DBID id : ids) {
      counter++;
      double lrdp = lrds.get(id);
      final Double lof;
      if(lrdp > 0) {
        List<DistanceResultPair<D>> neighbors = neigh1.get(id);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        // skip the point itself
        // neighbors.remove(0);
        double sum = 0;
        for(DistanceResultPair<D> neighbor1 : neighbors) {
          if(objectIsInKNN || neighbor1.getID() != id) {
            double lrdo = lrds.get(neighbor1.getSecond());
            sum += lrdo / lrdp;
          }
        }
        lof = sum / nsize;
      }
      else {
        lof = 1.0;
      }
      lofs.put(id, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      if(progressLOFs != null) {
        progressLOFs.setProcessed(counter, logger);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(logger);
    }
    return new Pair<WritableDataStore<Double>, MinMax<Double>>(lofs, lofminmax);
  }

  /**
   * Encapsulates information like the neighborhood, the LRD and LOF values of
   * the objects during a run of the {@link LOF} algorithm.
   */
  public class LOFResult {
    /**
     * The result of the run of the {@link LOF} algorithm.
     */
    private OutlierResult result;

    /**
     * The neighborhood of the objects w.r.t. the primary distance.
     */
    private KNNQuery.Instance<O, D> neigh1;

    /**
     * The neighborhood of the objects w.r.t. the reachability distance.
     */
    private KNNQuery.Instance<O, D> neigh2;

    /**
     * The LRD values of the objects.
     */
    private WritableDataStore<Double> lrds;

    /**
     * The LOF values of the objects.
     */
    private WritableDataStore<Double> lofs;

    /**
     * Encapsulates information generated during a run of the {@link LOF}
     * algorithm.
     * 
     * @param result the result of the run of the {@link LOF} algorithm
     * @param neigh1 the neighborhood of the objects w.r.t. the primary distance
     * @param neigh2 the neighborhood of the objects w.r.t. the reachability
     *        distance
     * @param lrds the LRD values of the objects
     * @param lofs the LOF values of the objects
     */
    public LOFResult(OutlierResult result, KNNQuery.Instance<O, D> neigh1, KNNQuery.Instance<O, D> neigh2, WritableDataStore<Double> lrds, WritableDataStore<Double> lofs) {
      this.result = result;
      this.neigh1 = neigh1;
      this.neigh2 = neigh2;
      this.lrds = lrds;
      this.lofs = lofs;
    }

    /**
     * @return the neighborhood of the objects w.r.t. the primary distance
     */
    public KNNQuery.Instance<O, D> getNeigh1() {
      return neigh1;
    }

    /**
     * @return the neighborhood of the objects w.r.t. the reachability distance
     */
    public KNNQuery.Instance<O, D> getNeigh2() {
      return neigh2;
    }

    /**
     * Get the first preprocessor.
     * 
     * @return Preprocessor instance
     */
    public MaterializeKNNPreprocessor<? super O, D>.Instance<O> getPreproc1() {
      if(PreprocessorKNNQuery.Instance.class.isInstance(neigh1)) {
        return ((PreprocessorKNNQuery.Instance<O, D>) neigh1).getPreprocessor();
      }
      return null;
    }

    /**
     * Get the second preprocessor.
     * 
     * @return Preprocessor instance
     */
    public MaterializeKNNPreprocessor<? super O, D>.Instance<O> getPreproc2() {
      if(PreprocessorKNNQuery.Instance.class.isInstance(neigh2)) {
        return ((PreprocessorKNNQuery.Instance<O, D>) neigh2).getPreprocessor();
      }
      return null;
    }

    /**
     * @return the LRD values of the objects
     */
    public WritableDataStore<Double> getLrds() {
      return lrds;
    }

    /**
     * @return the LOF values of the objects
     */
    public WritableDataStore<Double> getLofs() {
      return lofs;
    }

    /**
     * @return the result of the run of the {@link LOF} algorithm
     */
    public OutlierResult getResult() {
      return result;
    }
  }
}