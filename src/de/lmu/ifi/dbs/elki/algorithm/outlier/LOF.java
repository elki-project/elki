package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
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
 * {@link de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm#DISTANCE_FUNCTION_ID}
 * , while the reference set used in reachability distance computation is
 * configured using {@link #REACHABILITY_DISTANCE_FUNCTION_ID}.
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
 * @author Peer Kröger
 * @author Erich Schubert
 * @author Elke Achtert
 * 
 * @apiviz.has LOFResult oneway - - computes
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url = "http://dx.doi.org/10.1145/342009.335388")
public class LOF<O, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LOF.class);

  /**
   * The distance function to determine the reachability distance between
   * database objects.
   */
  public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("lof.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

  /**
   * The association id to associate the LOF_SCORE of an object for the
   * LOF_SCORE algorithm.
   */
  public static final AssociationID<Double> LOF_SCORE = AssociationID.getOrCreateAssociationID("lof", Double.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its LOF_SCORE, must be an integer greater than 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("lof.k", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  protected int k = 2;

  /**
   * Neighborhood distance function.
   */
  protected DistanceFunction<O, D> neighborhoodDistanceFunction;

  /**
   * Reachability distance function.
   */
  protected DistanceFunction<O, D> reachabilityDistanceFunction;

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  protected static boolean objectIsInKNN = false;

  /**
   * Constructor.
   * 
   * @param k the value of k
   * @param neighborhoodDistanceFunction the neighborhood distance function
   * @param reachabilityDistanceFunction the reachability distance function
   */
  public LOF(int k, DistanceFunction<O, D> neighborhoodDistanceFunction, DistanceFunction<O, D> reachabilityDistanceFunction) {
    super();
    this.k = k;
    this.neighborhoodDistanceFunction = neighborhoodDistanceFunction;
    this.reachabilityDistanceFunction = reachabilityDistanceFunction;
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@code #doRunInTime(Database)}.
   */
  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress("LOF", 3) : null;
    Pair<KNNQuery<O, D>, KNNQuery<O, D>> pair = getKNNQueries(database, stepprog);
    KNNQuery<O, D> kNNRefer = pair.getFirst();
    KNNQuery<O, D> kNNReach = pair.getSecond();
    return doRunInTime(database, kNNRefer, kNNReach, stepprog).getResult();
  }

  /**
   * Get the kNN queries for the algorithm.
   * 
   * @param database the database
   * @param stepprog the progress logger
   * @return the kNN queries for the algorithm
   */
  private Pair<KNNQuery<O, D>, KNNQuery<O, D>> getKNNQueries(Database database, StepProgress stepprog) {
    Relation<O> dataQuery = getRelation(database);
    // "HEAVY" flag for knnReach since it is used more than once
    KNNQuery<O, D> knnReach = database.getKNNQuery(dataQuery, reachabilityDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(knnReach == null) {
      if(stepprog != null) {
        if(neighborhoodDistanceFunction.equals(reachabilityDistanceFunction)) {
          stepprog.beginStep(1, "Materializing neighborhoods w.r.t. reference neighborhood distance function.", logger);
        }
        else {
          stepprog.beginStep(1, "Not materializing neighborhoods w.r.t. reference neighborhood distance function, but materializing neighborhoods w.r.t. reachability distance function.", logger);
        }
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(dataQuery, reachabilityDistanceFunction, k);
      database.addIndex(preproc);
      knnReach = preproc.getKNNQuery(reachabilityDistanceFunction, k);
    }

    // knnReach is only used once
    KNNQuery<O, D> knnRefer;
    if(neighborhoodDistanceFunction.equals(reachabilityDistanceFunction)) {
      knnRefer = knnReach;
    }
    else {
      // do not materialize the first neighborhood, since it is used only once
      knnRefer = database.getKNNQuery(dataQuery, neighborhoodDistanceFunction, k);
    }

    return new Pair<KNNQuery<O, D>, KNNQuery<O, D>>(knnRefer, knnReach);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database and
   * returns a {@link LOF.LOFResult} encapsulating information that may be
   * needed by an OnlineLOF algorithm.
   * 
   * @param database the database to process
   * @param kNNRefer the kNN query w.r.t. reference neighborhood distance
   *        function
   * @param kNNReach the kNN query w.r.t. reachability distance function
   */
  protected LOFResult<O, D> doRunInTime(Database database, KNNQuery<O, D> kNNRefer, KNNQuery<O, D> kNNReach, StepProgress stepprog) throws IllegalStateException {
    // Assert we got something
    if(kNNRefer == null) {
      throw new AbortException("No kNN queries supported by database for reference neighborhood distance function.");
    }
    if(kNNReach == null) {
      throw new AbortException("No kNN queries supported by database for reachability distance function.");
    }

    // Compute LRDs
    if(stepprog != null) {
      stepprog.beginStep(2, "Computing LRDs.", logger);
    }
    WritableDataStore<Double> lrds = computeLRDs(database.getDBIDs(), kNNReach);

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LOFs.", logger);
    }
    Pair<WritableDataStore<Double>, MinMax<Double>> lofsAndMax = computeLOFs(database.getDBIDs(), lrds, kNNRefer);
    WritableDataStore<Double> lofs = lofsAndMax.getFirst();
    // track the maximum value for normalization.
    MinMax<Double> lofminmax = lofsAndMax.getSecond();

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // Build result representation.
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("Local Outlier Factor", "lof-outlier", LOF_SCORE, lofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return new LOFResult<O, D>(database, result, kNNRefer, kNNReach, lrds, lofs);
  }

  /**
   * Computes the local reachability density (LRD) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param knnReach the precomputed neighborhood of the objects w.r.t. the
   *        reachability distance
   * @return the LRDs of the objects
   */
  protected WritableDataStore<Double> computeLRDs(DBIDs ids, KNNQuery<O, D> knnReach) {
    WritableDataStore<Double> lrds = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    FiniteProgress lrdsProgress = logger.isVerbose() ? new FiniteProgress("LRD", ids.size(), logger) : null;
    int counter = 0;
    for(DBID id : ids) {
      counter++;
      double sum = 0;
      List<DistanceResultPair<D>> neighbors = knnReach.getKNNForDBID(id, k);
      int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
      for(DistanceResultPair<D> neighbor : neighbors) {
        if(objectIsInKNN || neighbor.getID() != id) {
          List<DistanceResultPair<D>> neighborsNeighbors = knnReach.getKNNForDBID(neighbor.getID(), k);
          sum += Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.get(neighborsNeighbors.size() - 1).getDistance().doubleValue());
        }
      }
      // Avoid division by 0
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
   * @param knnRefer the precomputed neighborhood of the objects w.r.t. the
   *        reference distance
   * @return the LOFs of the objects and the maximum LOF
   */
  protected Pair<WritableDataStore<Double>, MinMax<Double>> computeLOFs(DBIDs ids, DataStore<Double> lrds, KNNQuery<O, D> knnRefer) {
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
        List<DistanceResultPair<D>> neighbors = knnRefer.getKNNForDBID(id, k);
        int nsize = neighbors.size() - (objectIsInKNN ? 0 : 1);
        // skip the point itself
        // neighbors.remove(0);
        double sum = 0;
        for(DistanceResultPair<D> neighbor : neighbors) {
          if(objectIsInKNN || neighbor.getID() != id) {
            sum += lrds.get(neighbor.getID());
          }
        }
        lof = (sum / nsize) / lrdp;
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

  @Override
  public TypeInformation getInputTypeRestriction() {
    if(reachabilityDistanceFunction.equals(neighborhoodDistanceFunction)) {
      return reachabilityDistanceFunction.getInputTypeRestriction();
    }
    return new CombinedTypeInformation(neighborhoodDistanceFunction.getInputTypeRestriction(), reachabilityDistanceFunction.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Encapsulates information like the neighborhood, the LRD and LOF values of
   * the objects during a run of the {@link LOF} algorithm.
   */
  public static class LOFResult<O, D extends NumberDistance<D, ?>> {
    /**
     * The database.
     */
    private final Database database;
  
    /**
     * The result of the run of the {@link LOF} algorithm.
     */
    private OutlierResult result;
  
    /**
     * The kNN query w.r.t. the reference neighborhood distance.
     */
    private final KNNQuery<O, D> kNNRefer;
  
    /**
     * The kNN query w.r.t. the reachability distance.
     */
    private final KNNQuery<O, D> kNNReach;
  
    /**
     * The RkNN query w.r.t. the reference neighborhood distance.
     */
    private RKNNQuery<O, D> rkNNRefer;
  
    /**
     * The rkNN query w.r.t. the reachability distance.
     */
    private RKNNQuery<O, D> rkNNReach;
  
    /**
     * The LRD values of the objects.
     */
    private final WritableDataStore<Double> lrds;
  
    /**
     * The LOF values of the objects.
     */
    private final WritableDataStore<Double> lofs;
  
    /**
     * Encapsulates information generated during a run of the {@link LOF}
     * algorithm.
     * 
     * @param result the result of the run of the {@link LOF} algorithm
     * @param kNNRefer the kNN query w.r.t. the reference neighborhood distance
     * @param kNNReach the kNN query w.r.t. the reachability distance
     * @param lrds the LRD values of the objects
     * @param lofs the LOF values of the objects
     */
    public LOFResult(Database database, OutlierResult result, KNNQuery<O, D> kNNRefer, KNNQuery<O, D> kNNReach, WritableDataStore<Double> lrds, WritableDataStore<Double> lofs) {
      this.database = database;
      this.result = result;
      this.kNNRefer = kNNRefer;
      this.kNNReach = kNNReach;
      this.lrds = lrds;
      this.lofs = lofs;
    }
  
    /**
     * @return the database
     */
    public Database getDatabase() {
      return database;
    }
  
    /**
     * @return the kNN query w.r.t. the reference neighborhood distance
     */
    public KNNQuery<O, D> getKNNRefer() {
      return kNNRefer;
    }
  
    /**
     * @return the kNN query w.r.t. the reachability distance
     */
    public KNNQuery<O, D> getKNNReach() {
      return kNNReach;
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
  
    /**
     * Sets the RkNN query w.r.t. the reference neighborhood distance.
     * 
     * @param rkNNRefer the query to set
     */
    public void setRkNNRefer(RKNNQuery<O, D> rkNNRefer) {
      this.rkNNRefer = rkNNRefer;
    }
  
    /**
     * @return the RkNN query w.r.t. the reference neighborhood distance
     */
    public RKNNQuery<O, D> getRkNNRefer() {
      return rkNNRefer;
    }
  
    /**
     * @return the RkNN query w.r.t. the reachability distance
     */
    public RKNNQuery<O, D> getRkNNReach() {
      return rkNNReach;
    }
  
    /**
     * Sets the RkNN query w.r.t. the reachability distance.
     * 
     * @param rkNNReach the query to set
     */
    public void setRkNNReach(RKNNQuery<O, D> rkNNReach) {
      this.rkNNReach = rkNNReach;
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * The neighborhood size to use
     */
    protected int k = 2;

    /**
     * Neighborhood distance function.
     */
    protected DistanceFunction<O, D> neighborhoodDistanceFunction = null;

    /**
     * Reachability distance function.
     */
    protected DistanceFunction<O, D> reachabilityDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID, new GreaterConstraint(1));
      if(config.grab(pK)) {
        k = pK.getValue();
      }

      final ObjectParameter<DistanceFunction<O, D>> reachDistP = new ObjectParameter<DistanceFunction<O, D>>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
      if(config.grab(reachDistP)) {
        reachabilityDistanceFunction = reachDistP.instantiateClass(config);
      }
    }

    @Override
    protected LOF<O, D> makeInstance() {
      // Default is to re-use the same distance
      DistanceFunction<O, D> rdist = (reachabilityDistanceFunction != null) ? reachabilityDistanceFunction : distanceFunction;
      return new LOF<O, D>(k + (objectIsInKNN ? 0 : 1), distanceFunction, rdist);
    }
  }
}