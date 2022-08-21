/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.outlier.lof;

import elki.Algorithm;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.database.query.knn.PreprocessorKNNQuery;
import elki.database.query.rknn.RKNNSearcher;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.DoubleMinMax;
import elki.math.MathUtil;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.QuotientOutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 * @has - - - KNNSearcher
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
public class FlexibleLOF<O> implements OutlierAlgorithm {
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
  protected Distance<? super O> referenceDistance;

  /**
   * Reachability distance function.
   */
  protected Distance<? super O> reachabilityDistance;

  /**
   * Constructor.
   *
   * @param krefer The number of neighbors for reference
   * @param kreach The number of neighbors for reachability distance
   * @param neighborhoodDistance the neighborhood distance function
   * @param reachabilityDistance the reachability distance function
   */
  public FlexibleLOF(int krefer, int kreach, Distance<? super O> neighborhoodDistance, Distance<? super O> reachabilityDistance) {
    super();
    this.krefer = krefer + 1;
    this.kreach = kreach + 1;
    this.referenceDistance = neighborhoodDistance;
    this.reachabilityDistance = reachabilityDistance;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(reachabilityDistance.equals(referenceDistance) ? reachabilityDistance.getInputTypeRestriction() : //
        new CombinedTypeInformation(reachabilityDistance.getInputTypeRestriction(), referenceDistance.getInputTypeRestriction()));
  }

  /**
   * Performs the Generalized LOF algorithm on the given database by calling
   * {@link #doRunInTime}.
   *
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LOF", 3) : null;
    DistanceQuery<O> distQ = new QueryBuilder<>(relation, reachabilityDistance).distanceQuery();
    // "HEAVY" flag for knnReach since it is used more than once
    KNNSearcher<DBIDRef> knnReach = new QueryBuilder<>(distQ).optimizedOnly().kNNByDBID(kreach);
    // No optimized kNN query - use a preprocessor!
    if(!(knnReach instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        stepprog.beginStep(1, referenceDistance.equals(reachabilityDistance) ? //
            "Materializing neighborhoods w.r.t. reference neighborhood distance." //
            : "Not materializing neighborhoods w.r.t. reference neighborhood distance, but materializing neighborhoods w.r.t. reachability distance function.", LOG);
      }
      knnReach = new QueryBuilder<>(relation, reachabilityDistance).precomputed().kNNByDBID(//
          (referenceDistance.equals(reachabilityDistance)) ? Math.max(kreach, krefer) : kreach);
    }

    // knnReach is only used once
    KNNSearcher<DBIDRef> knnRefer = knnReach;
    if(!referenceDistance.equals(reachabilityDistance)) {
      // do not materialize the first neighborhood, since it is used only once
      knnRefer = new QueryBuilder<>(relation, referenceDistance).kNNByDBID(krefer);
    }
    return doRunInTime(relation.getDBIDs(), knnRefer, knnReach, stepprog).getResult();
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
  protected LOFResult<O> doRunInTime(DBIDs ids, KNNSearcher<DBIDRef> kNNRefer, KNNSearcher<DBIDRef> kNNReach, StepProgress stepprog) {
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
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Factor", ids, lofs);
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
  protected void computeLRDs(KNNSearcher<DBIDRef> knnq, DBIDs ids, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("LRD", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNN(iter, kreach);
      double sum = 0.0;
      int count = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iter)) {
          continue;
        }
        KNNList neighborsNeighbors = knnq.getKNN(neighbor, kreach);
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
  protected void computeLOFs(KNNSearcher<DBIDRef> knnq, DBIDs ids, DoubleDataStore lrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lof, lrdp = lrds.doubleValue(iter);
      final KNNList neighbors = knnq.getKNN(iter, krefer);
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
    private final KNNSearcher<DBIDRef> kNNRefer;

    /**
     * The kNN query w.r.t. the reachability distance.
     */
    private final KNNSearcher<DBIDRef> kNNReach;

    /**
     * The RkNN query w.r.t. the reference neighborhood distance.
     */
    private RKNNSearcher<DBIDRef> rkNNRefer;

    /**
     * The rkNN query w.r.t. the reachability distance.
     */
    private RKNNSearcher<DBIDRef> rkNNReach;

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
    public LOFResult(OutlierResult result, KNNSearcher<DBIDRef> kNNRefer, KNNSearcher<DBIDRef> kNNReach, WritableDoubleDataStore lrds, WritableDoubleDataStore lofs) {
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
    public KNNSearcher<DBIDRef> getKNNRefer() {
      return kNNRefer;
    }

    /**
     * Get the knn query for the reachability set.
     *
     * @return the kNN query w.r.t. the reachability distance
     */
    public KNNSearcher<DBIDRef> getKNNReach() {
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
    public void setRkNNRefer(RKNNSearcher<DBIDRef> rkNNRefer) {
      this.rkNNRefer = rkNNRefer;
    }

    /**
     * Get the RkNN query for the reference set.
     *
     * @return the RkNN query w.r.t. the reference neighborhood distance
     */
    public RKNNSearcher<DBIDRef> getRkNNRefer() {
      return rkNNRefer;
    }

    /**
     * Get the RkNN query for the reachability set.
     *
     * @return the RkNN query w.r.t. the reachability distance
     */
    public RKNNSearcher<DBIDRef> getRkNNReach() {
      return rkNNReach;
    }

    /**
     * Sets the RkNN query w.r.t. the reachability distance.
     *
     * @param rkNNReach the query to set
     */
    public void setRkNNReach(RKNNSearcher<DBIDRef> rkNNReach) {
      this.rkNNReach = rkNNReach;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> implements Parameterizer {
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
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * Reachability distance function.
     */
    protected Distance<? super O> reachabilityDistance = null;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(KREF_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> krefer = x);
      kreach = krefer;
      new IntParameter(KREACH_ID)//
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> kreach = x);
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      reachabilityDistance = distance;
      new ObjectParameter<Distance<O>>(REACHABILITY_DISTANCE_FUNCTION_ID, Distance.class) //
          .setOptional(true) //
          .grab(config, x -> reachabilityDistance = x);
    }

    @Override
    public FlexibleLOF<O> make() {
      return new FlexibleLOF<>(kreach, krefer, distance, reachabilityDistance);
    }
  }
}
