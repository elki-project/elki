package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.CombinedTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
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
 * <p>
 * Flexible variant of the "Local Outlier Factor" algorithm.
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
 * configured using {@link Parameterizer#REACHABILITY_DISTANCE_FUNCTION_ID}.
 * </p>
 * 
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;. For consistency
 * with the name "kNN query", we chose to rename the parameter to {@code k}.
 * Flexible LOF allows you to set the two values different, which yields the
 * parameters {@link Parameterizer#KREF_ID} ({@code -lof.krefer}) and
 * {@link Parameterizer#KREACH_ID} ({@code -lof.kreach})
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
public class FlexibleLOF<O, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
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
  protected DistanceFunction<? super O, D> referenceDistanceFunction;

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
   * @param krefer The number of neighbors for reference
   * @param kreach The number of neighbors for reachability distance
   * @param neighborhoodDistanceFunction the neighborhood distance function
   * @param reachabilityDistanceFunction the reachability distance function
   */
  public FlexibleLOF(int krefer, int kreach, DistanceFunction<? super O, D> neighborhoodDistanceFunction, DistanceFunction<? super O, D> reachabilityDistanceFunction) {
    super();
    this.krefer = krefer + (objectIsInKNN ? 0 : 1);
    this.kreach = kreach + (objectIsInKNN ? 0 : 1);
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
    Pair<KNNQuery<O, D>, KNNQuery<O, D>> pair = getKNNQueries(database, relation, stepprog);
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
  private Pair<KNNQuery<O, D>, KNNQuery<O, D>> getKNNQueries(Database database, Relation<O> relation, StepProgress stepprog) {
    // "HEAVY" flag for knnReach since it is used more than once
    KNNQuery<O, D> knnReach = QueryUtil.getKNNQuery(relation, reachabilityDistanceFunction, kreach, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if (!(knnReach instanceof PreprocessorKNNQuery)) {
      if (stepprog != null) {
        if (referenceDistanceFunction.equals(reachabilityDistanceFunction)) {
          stepprog.beginStep(1, "Materializing neighborhoods w.r.t. reference neighborhood distance function.", LOG);
        } else {
          stepprog.beginStep(1, "Not materializing neighborhoods w.r.t. reference neighborhood distance function, but materializing neighborhoods w.r.t. reachability distance function.", LOG);
        }
      }
      int kpreproc = (referenceDistanceFunction.equals(reachabilityDistanceFunction)) ? Math.max(kreach, krefer) : kreach;
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(relation, reachabilityDistanceFunction, kpreproc);
      database.addIndex(preproc);
      DistanceQuery<O, D> rdq = database.getDistanceQuery(relation, reachabilityDistanceFunction);
      knnReach = preproc.getKNNQuery(rdq, kreach);
    }

    // knnReach is only used once
    KNNQuery<O, D> knnRefer;
    if (referenceDistanceFunction == reachabilityDistanceFunction || referenceDistanceFunction.equals(reachabilityDistanceFunction)) {
      knnRefer = knnReach;
    } else {
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
  protected LOFResult<O, D> doRunInTime(DBIDs ids, KNNQuery<O, D> kNNRefer, KNNQuery<O, D> kNNReach, StepProgress stepprog) {
    // Assert we got something
    if (kNNRefer == null) {
      throw new AbortException("No kNN queries supported by database for reference neighborhood distance function.");
    }
    if (kNNReach == null) {
      throw new AbortException("No kNN queries supported by database for reachability distance function.");
    }

    // Compute LRDs
    if (stepprog != null) {
      stepprog.beginStep(2, "Computing LRDs.", LOG);
    }
    WritableDoubleDataStore lrds = computeLRDs(ids, kNNReach);

    // compute LOF_SCORE of each db object
    if (stepprog != null) {
      stepprog.beginStep(3, "Computing LOFs.", LOG);
    }
    Pair<WritableDoubleDataStore, DoubleMinMax> lofsAndMax = computeLOFs(ids, lrds, kNNRefer);
    WritableDoubleDataStore lofs = lofsAndMax.getFirst();
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = lofsAndMax.getSecond();

    if (stepprog != null) {
      stepprog.setCompleted(LOG);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Local Outlier Factor", "lof-outlier", TypeUtil.DOUBLE, lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return new LOFResult<>(result, kNNRefer, kNNReach, lrds, lofs);
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
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("LRD", ids.size(), LOG) : null;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList<D> neighbors = knnReach.getKNNForDBID(iter, kreach);
      double sum = 0.0;
      int count = 0;
      if (neighbors instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for (DoubleDistanceDBIDListIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance()) {
          if (objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            KNNList<D> neighborsNeighbors = knnReach.getKNNForDBID(neighbor, kreach);
            final double nkdist;
            if (neighborsNeighbors instanceof DoubleDistanceKNNList) {
              nkdist = ((DoubleDistanceKNNList) neighborsNeighbors).doubleKNNDistance();
            } else {
              nkdist = neighborsNeighbors.getKNNDistance().doubleValue();
            }
            sum += Math.max(neighbor.doubleDistance(), nkdist);
            count++;
          }
        }
      } else {
        for (DistanceDBIDListIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if (objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            KNNList<D> neighborsNeighbors = knnReach.getKNNForDBID(neighbor, kreach);
            sum += Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.getKNNDistance().doubleValue());
            count++;
          }
        }
      }
      // Avoid division by 0
      final double lrd = (sum > 0) ? (count / sum) : Double.POSITIVE_INFINITY;
      lrds.putDouble(iter, lrd);
      if (lrdsProgress != null) {
        lrdsProgress.incrementProcessed(LOG);
      }
    }
    if (lrdsProgress != null) {
      lrdsProgress.ensureCompleted(LOG);
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

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lrdp = lrds.doubleValue(iter);
      final double lof;
      if (lrdp > 0 && !Double.isInfinite(lrdp)) {
        final KNNList<D> neighbors = knnRefer.getKNNForDBID(iter, krefer);
        double sum = 0.0;
        int count = 0;
        for (DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if (objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            sum += lrds.doubleValue(neighbor);
            count++;
          }
        }
        lof = sum / (count * lrdp);
      } else {
        lof = 1.0;
      }
      lofs.putDouble(iter, lof);
      // update minimum and maximum
      if (!Double.isInfinite(lof)) {
        lofminmax.put(lof);
      }

      if (progressLOFs != null) {
        progressLOFs.incrementProcessed(LOG);
      }
    }
    if (progressLOFs != null) {
      progressLOFs.ensureCompleted(LOG);
    }
    return new Pair<>(lofs, lofminmax);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    final TypeInformation type;
    if (reachabilityDistanceFunction.equals(referenceDistanceFunction)) {
      type = reachabilityDistanceFunction.getInputTypeRestriction();
    } else {
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
  public static class LOFResult<O, D extends NumberDistance<D, ?>> {
    /**
     * The result of the run of the {@link FlexibleLOF} algorithm.
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
     * Encapsulates information generated during a run of the
     * {@link FlexibleLOF} algorithm.
     * 
     * @param result the result of the run of the {@link FlexibleLOF} algorithm
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
     * Get the knn query for the reference set.
     * 
     * @return the kNN query w.r.t. the reference neighborhood distance
     */
    public KNNQuery<O, D> getKNNRefer() {
      return kNNRefer;
    }

    /**
     * Get the knn query for the reachability set.
     * 
     * @return the kNN query w.r.t. the reachability distance
     */
    public KNNQuery<O, D> getKNNReach() {
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
    public void setRkNNRefer(RKNNQuery<O, D> rkNNRefer) {
      this.rkNNRefer = rkNNRefer;
    }

    /**
     * Get the RkNN query for the reference set.
     * 
     * @return the RkNN query w.r.t. the reference neighborhood distance
     */
    public RKNNQuery<O, D> getRkNNRefer() {
      return rkNNRefer;
    }

    /**
     * Get the RkNN query for the reachability set.
     * 
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
     * The distance function to determine the reachability distance between
     * database objects.
     */
    public static final OptionID REACHABILITY_DISTANCE_FUNCTION_ID = new OptionID("lof.reachdistfunction", "Distance function to determine the reachability distance between database objects.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOF_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID KREF_ID = new OptionID("lof.krefer", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its reachability distance.
     */
    public static final OptionID KREACH_ID = new OptionID("lof.kreach", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

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
    protected DistanceFunction<O, D> neighborhoodDistanceFunction = null;

    /**
     * Reachability distance function.
     */
    protected DistanceFunction<O, D> reachabilityDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(KREF_ID);
      pK.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if (config.grab(pK)) {
        krefer = pK.intValue();
      }

      final IntParameter pK2 = new IntParameter(KREACH_ID);
      pK2.setOptional(true);
      pK2.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if (config.grab(pK2)) {
        kreach = pK2.intValue();
      } else {
        kreach = krefer;
      }

      final ObjectParameter<DistanceFunction<O, D>> reachDistP = new ObjectParameter<>(REACHABILITY_DISTANCE_FUNCTION_ID, DistanceFunction.class);
      reachDistP.setOptional(true);
      if (config.grab(reachDistP)) {
        reachabilityDistanceFunction = reachDistP.instantiateClass(config);
      } else {
        reachabilityDistanceFunction = distanceFunction;
      }
    }

    @Override
    protected FlexibleLOF<O, D> makeInstance() {
      return new FlexibleLOF<>(kreach, krefer, distanceFunction, reachabilityDistanceFunction);
    }
  }
}
