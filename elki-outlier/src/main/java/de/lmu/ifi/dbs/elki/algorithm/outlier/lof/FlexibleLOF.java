/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Flexible variant of the "Local Outlier Factor" algorithm.
 * <p>
 * This implementation diverts from the original LOF publication in that it
 * allows the user to use a different distance function for the reachability
 * distance and neighborhood determination (although the default is to use the
 * same value.)
 * <p>
 * The k nearest neighbors are determined using the standard distance function,
 * while the reference set used in reachability distance computation is
 * configured using a separate reachability distance function.
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;. For consistency
 * with the name "kNN query", we chose to rename the parameter to {@code k}.
 * Flexible LOF allows you to set the two values different, which yields the
 * parameters {@code -lof.krefer} and {@code -lof.kreach}.
 * <p>
 * Reference:<br>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander<br>
 * LOF: Identifying Density-Based Local Outliers<br>
 * Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00)
 *
 * @author Peer Kröger
 * @author Erich Schubert
 * @author Elke Achtert
 * @since 0.2
 *
 * @navhas - computes - LOFResult
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("FlexibleLOF: Local Outlier Factor with additional options")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander", //
    title = "LOF: Identifying Density-Based Local Outliers", //
    booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00)", //
    url = "https://doi.org/10.1145/342009.335388", //
    bibkey = "DBLP:conf/sigmod/BreunigKNS00")
public class FlexibleLOF<O> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FlexibleLOF.class);

  /**
   * Number of neighbors in comparison set.
   */
  protected int krefer = 2;

  /**
   * Number of neighbors used for reachability distance.
   */
  protected int kreach = 2;

  /**
   * Neighborhood distance function.
   */
  protected DistanceFunction<? super O> referenceDistanceFunction;

  /**
   * Reachability distance function.
   */
  protected DistanceFunction<? super O> reachabilityDistanceFunction;

  /**
   * Constructor.
   *
   * @param krefer The number of neighbors for reference
   * @param kreach The number of neighbors for reachability distance
   * @param neighborhoodDistanceFunction the neighborhood distance function
   * @param reachabilityDistanceFunction the reachability distance function
   */
  public FlexibleLOF(int krefer, int kreach, DistanceFunction<? super O> neighborhoodDistanceFunction, DistanceFunction<? super O> reachabilityDistanceFunction) {
    super();
    this.krefer = krefer + 1;
    this.kreach = kreach + 1;
    this.referenceDistanceFunction = neighborhoodDistanceFunction;
    this.reachabilityDistanceFunction = reachabilityDistanceFunction;
  }

  /**
   * Performs the Generalized LOF algorithm on the given database by calling
   * {@link #doRunInTime}.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LOF", 3) : null;
    Pair<KNNQuery<O>, KNNQuery<O>> pair = getKNNQueries(database, relation, stepprog);
    KNNQuery<O> kNNRefer = pair.getFirst();
    KNNQuery<O> kNNReach = pair.getSecond();
    return doRunInTime(relation.getDBIDs(), kNNRefer, kNNReach, stepprog).getResult();
  }

  /**
   * Get the kNN queries for the algorithm.
   *
   * @param relation the data
   * @param stepprog the progress logger
   * @return the kNN queries for the algorithm
   */
  private Pair<KNNQuery<O>, KNNQuery<O>> getKNNQueries(Database database, Relation<O> relation, StepProgress stepprog) {
    DistanceQuery<O> distQ = database.getDistanceQuery(relation, reachabilityDistanceFunction, DatabaseQuery.HINT_HEAVY_USE);
    // "HEAVY" flag for knnReach since it is used more than once
    KNNQuery<O> knnReach = database.getKNNQuery(distQ, kreach, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnReach instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        if(referenceDistanceFunction.equals(reachabilityDistanceFunction)) {
          stepprog.beginStep(1, "Materializing neighborhoods w.r.t. reference neighborhood distance function.", LOG);
        }
        else {
          stepprog.beginStep(1, "Not materializing neighborhoods w.r.t. reference neighborhood distance function, but materializing neighborhoods w.r.t. reachability distance function.", LOG);
        }
      }
      int kpreproc = (referenceDistanceFunction.equals(reachabilityDistanceFunction)) ? Math.max(kreach, krefer) : kreach;
      knnReach = DatabaseUtil.precomputedKNNQuery(database, relation, reachabilityDistanceFunction, kpreproc);
    }

    // knnReach is only used once
    KNNQuery<O> knnRefer;
    if(referenceDistanceFunction == reachabilityDistanceFunction || referenceDistanceFunction.equals(reachabilityDistanceFunction)) {
      knnRefer = knnReach;
    }
    else {
      // do not materialize the first neighborhood, since it is used only once
      knnRefer = QueryUtil.getKNNQuery(relation, referenceDistanceFunction, krefer);
    }

    return new Pair<>(knnRefer, knnReach);
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database and
   * returns a {@link FlexibleLOF.LOFResult} encapsulating information that may
   * be needed by an OnlineLOF algorithm.
   *
   * @param ids Object ids
   * @param kNNRefer the kNN query w.r.t. reference neighborhood distance
   *        function
   * @param kNNReach the kNN query w.r.t. reachability distance function
   * @param stepprog Progress logger
   * @return LOF result
   */
  protected LOFResult<O> doRunInTime(DBIDs ids, KNNQuery<O> kNNRefer, KNNQuery<O> kNNReach, StepProgress stepprog) {
    // Assert we got something
    if(kNNRefer == null) {
      throw new AbortException("No kNN queries supported by database for reference neighborhood distance function.");
    }
    if(kNNReach == null) {
      throw new AbortException("No kNN queries supported by database for reachability distance function.");
    }

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing LRDs.");
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeLRDs(kNNReach, ids, lrds);

    // compute LOF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing LOFs.");
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();
    computeLOFs(kNNRefer, ids, lrds, lofs, lofminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Factor", "lof-outlier", lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    return new LOFResult<>(result, kNNRefer, kNNReach, lrds, lofs);
  }

  /**
   * Computes the local reachability density (LRD) of the specified objects.
   *
   * @param knnq the precomputed neighborhood of the objects w.r.t. the
   *        reachability distance
   * @param ids the ids of the objects
   * @param lrds Reachability storage
   */
  protected void computeLRDs(KNNQuery<O> knnq, DBIDs ids, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("LRD", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, kreach);
      double sum = 0.0;
      int count = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iter)) {
          continue;
        }
        KNNList neighborsNeighbors = knnq.getKNNForDBID(neighbor, kreach);
        sum += MathUtil.max(neighbor.doubleValue(), neighborsNeighbors.getKNNDistance());
        count++;
      }
      // Avoid division by 0
      final double lrd = (sum > 0) ? (count / sum) : Double.POSITIVE_INFINITY;
      lrds.putDouble(iter, lrd);
      LOG.incrementProcessed(lrdsProgress);
    }
    LOG.ensureCompleted(lrdsProgress);
  }

  /**
   * Computes the Local outlier factor (LOF) of the specified objects.
   *
   * @param knnq the precomputed neighborhood of the objects w.r.t. the
   *        reference distance
   * @param ids IDs to process
   * @param lrds Local reachability distances
   * @param lofs Local outlier factor storage
   * @param lofminmax Score minimum/maximum tracker
   */
  protected void computeLOFs(KNNQuery<O> knnq, DBIDs ids, DoubleDataStore lrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lof;
      final double lrdp = lrds.doubleValue(iter);
      final KNNList neighbors = knnq.getKNNForDBID(iter, krefer);
      if(!Double.isInfinite(lrdp)) {
        double sum = 0.;
        int count = 0;
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if(DBIDUtil.equal(neighbor, iter)) {
            continue;
          }
          final double val = lrds.doubleValue(neighbor);
          sum += val;
          count++;
          if(Double.isInfinite(val)) {
            break;
          }
        }
        lof = sum / (lrdp * count);
      }
      else {
        lof = 1.0;
      }
      lofs.putDouble(iter, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      LOG.incrementProcessed(progressLOFs);
    }
    LOG.ensureCompleted(progressLOFs);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation type;
    if(reachabilityDistanceFunction.equals(referenceDistanceFunction)) {
      type = reachabilityDistanceFunction.getInputTypeRestriction();
    }
    else {
      type = new CombinedTypeInformation(referenceDistanceFunction.getInputTypeRestriction(), reachabilityDistanceFunction.getInputTypeRestriction());
    }
    return TypeUtil.array(type);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Encapsulates information like the neighborhood, the LRD and LOF values of
   * the objects during a run of the {@link FlexibleLOF} algorithm.
   *
   * @author Elke Achtert
   */
  public static class LOFResult<O> {
    /**
     * The result of the run of the {@link FlexibleLOF} algorithm.
     */
    private OutlierResult result;

    /**
     * The kNN query w.r.t. the reference neighborhood distance.
     */
    private final KNNQuery<O> kNNRefer;

    /**
     * The kNN query w.r.t. the reachability distance.
     */
    private final KNNQuery<O> kNNReach;

    /**
     * The RkNN query w.r.t. the reference neighborhood distance.
     */
    private RKNNQuery<O> rkNNRefer;

    /**
     * The rkNN query w.r.t. the reachability distance.
     */
    private RKNNQuery<O> rkNNReach;

    /**
     * The LRD values of the objects.
     */
    private final WritableDoubleDataStore lrds;

    /**
     * The LOF values of the objects.
     */
    private final WritableDoubleDataStore lofs;

    /**
     * Encapsulates information generated during a run of the
     * {@link FlexibleLOF} algorithm.
     *
     * @param result the result of the run of the {@link FlexibleLOF} algorithm
     * @param kNNRefer the kNN query w.r.t. the reference neighborhood distance
     * @param kNNReach the kNN query w.r.t. the reachability distance
     * @param lrds the LRD values of the objects
     * @param lofs the LOF values of the objects
     */
    public LOFResult(OutlierResult result, KNNQuery<O> kNNRefer, KNNQuery<O> kNNReach, WritableDoubleDataStore lrds, WritableDoubleDataStore lofs) {
      this.result = result;
      this.kNNRefer = kNNRefer;
      this.kNNReach = kNNReach;
      this.lrds = lrds;
      this.lofs = lofs;
    }

    /**
     * Get the knn query for the reference set.
     *
     * @return the kNN query w.r.t. the reference neighborhood distance
     */
    public KNNQuery<O> getKNNRefer() {
      return kNNRefer;
    }

    /**
     * Get the knn query for the reachability set.
     *
     * @return the kNN query w.r.t. the reachability distance
     */
    public KNNQuery<O> getKNNReach() {
      return kNNReach;
    }

    /**
     * Get the LRD data store.
     *
     * @return the LRD values of the objects
     */
    public WritableDoubleDataStore getLrds() {
      return lrds;
    }

    /**
     * Get the LOF data store.
     *
     * @return the LOF values of the objects
     */
    public WritableDoubleDataStore getLofs() {
      return lofs;
    }

    /**
     * Get the outlier result.
     *
     * @return the result of the run of the {@link FlexibleLOF} algorithm
     */
    public OutlierResult getResult() {
      return result;
    }

    /**
     * Sets the RkNN query w.r.t. the reference neighborhood distance.
     *
     * @param rkNNRefer the query to set
     */
    public void setRkNNRefer(RKNNQuery<O> rkNNRefer) {
      this.rkNNRefer = rkNNRefer;
    }

    /**
     * Get the RkNN query for the reference set.
     *
     * @return the RkNN query w.r.t. the reference neighborhood distance
     */
    public RKNNQuery<O> getRkNNRefer() {
      return rkNNRefer;
    }

    /**
     * Get the RkNN query for the reachability set.
     *
     * @return the RkNN query w.r.t. the reachability distance
     */
    public RKNNQuery<O> getRkNNReach() {
      return rkNNReach;
    }

    /**
     * Sets the RkNN query w.r.t. the reachability distance.
     *
     * @param rkNNReach the query to set
     */
    public void setRkNNReach(RKNNQuery<O> rkNNReach) {
      this.rkNNReach = rkNNReach;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * The distance function to determine the reachability distance between
     * database objects.
     */
    public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = new OptionID("lof.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOF score, must be an integer greater or
     * equal to 1.
     */
    public static final OptionID KREF_ID = new OptionID("lof.krefer", "The number of nearest neighbors of an object to be considered for computing its LOF score.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its reachability distance.
     */
    public static final OptionID KREACH_ID = new OptionID("lof.kreach", "The number of nearest neighbors of an object to be considered for computing its LOF score.");

    /**
     * The reference set size to use.
     */
    protected int krefer = 2;

    /**
     * The set size to use for reachability distance.
     */
    protected int kreach = 2;

    /**
     * Neighborhood distance function.
     */
    protected DistanceFunction<? super O> neighborhoodDistanceFunction = null;

    /**
     * Reachability distance function.
     */
    protected DistanceFunction<? super O> reachabilityDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter pK = new IntParameter(KREF_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK)) {
        krefer = pK.intValue();
      }

      IntParameter pK2 = new IntParameter(KREACH_ID)//
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK2)) {
        kreach = pK2.intValue();
      }
      else {
        kreach = krefer;
      }

      ObjectParameter<DistanceFunction<O>> reachDistP = new ObjectParameter<>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class);
      reachDistP.setOptional(true);
      if(config.grab(reachDistP)) {
        reachabilityDistanceFunction = reachDistP.instantiateClass(config);
      }
      else {
        reachabilityDistanceFunction = distanceFunction;
      }
    }

    @Override
    protected FlexibleLOF<O> makeInstance() {
      return new FlexibleLOF<>(kreach, krefer, distanceFunction, reachabilityDistanceFunction);
    }
  }
}
