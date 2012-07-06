package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
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
public class LOF<O, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
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
  protected DistanceFunction<? super O, D> neighborhoodDistanceFunction;

  /**
   * Reachability distance function.
   */
  protected DistanceFunction<? super O, D> reachabilityDistanceFunction;

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  private static boolean objectIsInKNN = false;

  /**
   * Constructor.
   * 
   * @param k the value of k
   * @param neighborhoodDistanceFunction the neighborhood distance function
   * @param reachabilityDistanceFunction the reachability distance function
   */
  public LOF(int k, DistanceFunction<? super O, D> neighborhoodDistanceFunction, DistanceFunction<? super O, D> reachabilityDistanceFunction) {
    super();
    this.k = k + (objectIsInKNN ? 0 : 1);
    this.neighborhoodDistanceFunction = neighborhoodDistanceFunction;
    this.reachabilityDistanceFunction = reachabilityDistanceFunction;
  }

  /**
   * Constructor.
   * 
   * @param k the value of k
   * @param distanceFunction the distance function
   * 
   *        Uses the same distance function for neighborhood computation and
   *        reachability distance (standard as in the original publication),
   *        same as {@link #LOF(int, DistanceFunction, DistanceFunction)
   *        LOF(int, distanceFunction, distanceFunction)}.
   */
  public LOF(int k, DistanceFunction<? super O, D> distanceFunction) {
    this(k, distanceFunction, distanceFunction);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database by
   * calling {@link #doRunInTime}.
   * 
   * @param relation Data to process
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress("LOF", 3) : null;
    Pair<KNNQuery<O, D>, KNNQuery<O, D>> pair = getKNNQueries(relation, stepprog);
    KNNQuery<O, D> kNNRefer = pair.getFirst();
    KNNQuery<O, D> kNNReach = pair.getSecond();
    return doRunInTime(relation.getDBIDs(), kNNRefer, kNNReach, stepprog).getResult();
  }

  /**
   * Get the kNN queries for the algorithm.
   * 
   * @param relation the data
   * @param stepprog the progress logger
   * @return the kNN queries for the algorithm
   */
  private Pair<KNNQuery<O, D>, KNNQuery<O, D>> getKNNQueries(Relation<O> relation, StepProgress stepprog) {
    // "HEAVY" flag for knnReach since it is used more than once
    KNNQuery<O, D> knnReach = QueryUtil.getKNNQuery(relation, reachabilityDistanceFunction, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnReach instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        if(neighborhoodDistanceFunction.equals(reachabilityDistanceFunction)) {
          stepprog.beginStep(1, "Materializing neighborhoods w.r.t. reference neighborhood distance function.", logger);
        }
        else {
          stepprog.beginStep(1, "Not materializing neighborhoods w.r.t. reference neighborhood distance function, but materializing neighborhoods w.r.t. reachability distance function.", logger);
        }
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(relation, reachabilityDistanceFunction, k);
      relation.getDatabase().addIndex(preproc);
      DistanceQuery<O, D> rdq = relation.getDatabase().getDistanceQuery(relation, reachabilityDistanceFunction);
      knnReach = preproc.getKNNQuery(rdq, k);
    }

    // knnReach is only used once
    KNNQuery<O, D> knnRefer;
    if(neighborhoodDistanceFunction == reachabilityDistanceFunction || neighborhoodDistanceFunction.equals(reachabilityDistanceFunction)) {
      knnRefer = knnReach;
    }
    else {
      // do not materialize the first neighborhood, since it is used only once
      knnRefer = QueryUtil.getKNNQuery(relation, neighborhoodDistanceFunction, k);
    }

    return new Pair<KNNQuery<O, D>, KNNQuery<O, D>>(knnRefer, knnReach);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database and
   * returns a {@link LOF.LOFResult} encapsulating information that may be
   * needed by an OnlineLOF algorithm.
   * 
   * @param ids Object ids
   * @param kNNRefer the kNN query w.r.t. reference neighborhood distance
   *        function
   * @param kNNReach the kNN query w.r.t. reachability distance function
   * @param stepprog Progress logger
   * @return LOF result
   */
  protected LOFResult<O, D> doRunInTime(DBIDs ids, KNNQuery<O, D> kNNRefer, KNNQuery<O, D> kNNReach, StepProgress stepprog) {
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
    WritableDoubleDataStore lrds = computeLRDs(ids, kNNReach);

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LOFs.", logger);
    }
    Pair<WritableDoubleDataStore, DoubleMinMax> lofsAndMax = computeLOFs(ids, lrds, kNNRefer);
    WritableDoubleDataStore lofs = lofsAndMax.getFirst();
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = lofsAndMax.getSecond();

    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<Double>("Local Outlier Factor", "lof-outlier", TypeUtil.DOUBLE, lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return new LOFResult<O, D>(result, kNNRefer, kNNReach, lrds, lofs);
  }

  /**
   * Computes the local reachability density (LRD) of the specified objects.
   * 
   * @param ids the ids of the objects
   * @param knnReach the precomputed neighborhood of the objects w.r.t. the
   *        reachability distance
   * @return the LRDs of the objects
   */
  protected WritableDoubleDataStore computeLRDs(DBIDs ids, KNNQuery<O, D> knnReach) {
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress lrdsProgress = logger.isVerbose() ? new FiniteProgress("LRD", ids.size(), logger) : null;
    Mean mean = new Mean();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      mean.reset();
      KNNResult<D> neighbors = knnReach.getKNNForDBID(iter, k);
      if(neighbors instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for(DoubleDistanceDBIDResultIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance()) {
          if(objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            DoubleDistanceKNNList neighborsNeighbors = (DoubleDistanceKNNList) knnReach.getKNNForDBID(neighbor, k);
            mean.put(Math.max(neighbor.doubleDistance(), neighborsNeighbors.doubleKNNDistance()));
          }
        }
      }
      else {
        for(DistanceDBIDResultIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            KNNResult<D> neighborsNeighbors = knnReach.getKNNForDBID(neighbor, k);
            mean.put(Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.getKNNDistance().doubleValue()));
          }
        }
      }
      // Avoid division by 0
      final double lrd = (mean.getCount() > 0) ? 1 / mean.getMean() : 0.0;
      lrds.putDouble(iter, lrd);
      if(lrdsProgress != null) {
        lrdsProgress.incrementProcessed(logger);
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
  protected Pair<WritableDoubleDataStore, DoubleMinMax> computeLOFs(DBIDs ids, DoubleDataStore lrds, KNNQuery<O, D> knnRefer) {
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = logger.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), logger) : null;
    Mean mean = new Mean();
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double lrdp = lrds.doubleValue(iter);
      final double lof;
      if(lrdp > 0) {
        final KNNResult<D> neighbors = knnRefer.getKNNForDBID(iter, k);
        mean.reset();
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if(objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            mean.put(lrds.doubleValue(neighbor));
          }
        }
        lof = mean.getMean() / lrdp;
      }
      else {
        lof = 1.0;
      }
      lofs.putDouble(iter, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      if(progressLOFs != null) {
        progressLOFs.incrementProcessed(logger);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(logger);
    }
    return new Pair<WritableDoubleDataStore, DoubleMinMax>(lofs, lofminmax);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation type;
    if(reachabilityDistanceFunction.equals(neighborhoodDistanceFunction)) {
      type = reachabilityDistanceFunction.getInputTypeRestriction();
    }
    else {
      type = new CombinedTypeInformation(neighborhoodDistanceFunction.getInputTypeRestriction(), reachabilityDistanceFunction.getInputTypeRestriction());
    }
    return TypeUtil.array(type);
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
    private final WritableDoubleDataStore lrds;

    /**
     * The LOF values of the objects.
     */
    private final WritableDoubleDataStore lofs;

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
    public LOFResult(OutlierResult result, KNNQuery<O, D> kNNRefer, KNNQuery<O, D> kNNReach, WritableDoubleDataStore lrds, WritableDoubleDataStore lofs) {
      this.result = result;
      this.kNNRefer = kNNRefer;
      this.kNNReach = kNNReach;
      this.lrds = lrds;
      this.lofs = lofs;
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
    public WritableDoubleDataStore getLrds() {
      return lrds;
    }

    /**
     * @return the LOF values of the objects
     */
    public WritableDoubleDataStore getLofs() {
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
      return new LOF<O, D>(k, distanceFunction, rdist);
    }
  }
}