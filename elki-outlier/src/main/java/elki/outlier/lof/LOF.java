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
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
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
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter {@code -lof.k}.
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;, but for consistency
 * within ELKI we have renamed this parameter to &quot;k&quot;.
 * <p>
 * Compatibility note: as of ELKI 0.7.0, we no longer include the query point,
 * for consistency with other methods.
 * <p>
 * Reference:
 * <p>
 * Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander<br>
 * LOF: Identifying Density-Based Local Outliers<br>
 * Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00)
 * 
 * @author Erich Schubert
 * @author Elke Achtert
 * @since 0.2
 * 
 * @has - - - KNNSearcher
 * 
 * @param <O> the type of data objects handled by this algorithm
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "Markus M. Breunig, Hans-Peter Kriegel, Raymond Ng, Jörg Sander", //
    title = "LOF: Identifying Density-Based Local Outliers", //
    booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00)", //
    url = "https://doi.org/10.1145/342009.335388", //
    bibkey = "DBLP:conf/sigmod/BreunigKNS00")
@Priority(Priority.RECOMMENDED)
public class LOF<O> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LOF.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * The number of neighbors to query (plus the query point!)
   */
  protected int kplus;

  /**
   * Constructor.
   * 
   * @param k the number of neighbors to use for comparison (excluding the query
   *        point)
   * @param distance the neighborhood distance function
   */
  public LOF(int k, Distance<? super O> distance) {
    super();
    this.distance = distance;
    this.kplus = k + 1; // + query point
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Runs the LOF algorithm on the given database.
   *
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LOF", 3) : null;
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Materializing nearest-neighbor sets.");
    KNNSearcher<DBIDRef> knnq = new QueryBuilder<>(relation, distance).precomputed().kNNByDBID(kplus);

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing Local Reachability Densities (LRD).");
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeLRDs(knnq, ids, lrds);

    // compute LOF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing Local Outlier Factors (LOF).");
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();
    computeLOFScores(knnq, ids, lrds, lofs, lofminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Factor", ids, lofs);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Compute local reachability distances.
   * 
   * @param knnq KNN query
   * @param ids IDs to process
   * @param lrds Reachability storage
   */
  private void computeLRDs(KNNSearcher<DBIDRef> knnq, DBIDs ids, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("Local Reachability Densities (LRD)", ids.size(), LOG) : null;
    double lrd;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      lrd = computeLRD(knnq, iter);
      lrds.putDouble(iter, lrd);
      LOG.incrementProcessed(lrdsProgress);
    }
    LOG.ensureCompleted(lrdsProgress);
  }

  /**
   * Compute a single local reachability distance.
   * 
   * @param knnq kNN Query
   * @param curr Current object
   * @return Local Reachability Density
   */
  protected double computeLRD(KNNSearcher<DBIDRef> knnq, DBIDIter curr) {
    final KNNList neighbors = knnq.getKNN(curr, kplus);
    double sum = 0.0;
    int count = 0;
    for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
      if(DBIDUtil.equal(curr, neighbor)) {
        continue;
      }
      KNNList neighborsNeighbors = knnq.getKNN(neighbor, kplus);
      sum += MathUtil.max(neighbor.doubleValue(), neighborsNeighbors.getKNNDistance());
      count++;
    }
    // Avoid division by 0
    return (sum > 0) ? (count / sum) : Double.POSITIVE_INFINITY;
  }

  /**
   * Compute local outlier factors.
   * 
   * @param knnq KNN query
   * @param ids IDs to process
   * @param lrds Local reachability distances
   * @param lofs Local outlier factor storage
   * @param lofminmax Score minimum/maximum tracker
   */
  private void computeLOFScores(KNNSearcher<DBIDRef> knnq, DBIDs ids, DoubleDataStore lrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("Local Outlier Factor (LOF) scores", ids.size(), LOG) : null;
    double lof;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      lof = computeLOFScore(knnq, iter, lrds);
      lofs.putDouble(iter, lof);
      // update minimum and maximum
      lofminmax.put(lof);
      LOG.incrementProcessed(progressLOFs);
    }
    LOG.ensureCompleted(progressLOFs);
  }

  /**
   * Compute a single LOF score.
   * 
   * @param knnq kNN query
   * @param cur Current object
   * @param lrds Stored reachability densities
   * @return LOF score.
   */
  protected double computeLOFScore(KNNSearcher<DBIDRef> knnq, DBIDRef cur, DoubleDataStore lrds) {
    final double lrdp = lrds.doubleValue(cur);
    if(Double.isInfinite(lrdp)) {
      return 1.0;
    }
    double sum = 0.;
    int count = 0;
    final KNNList neighbors = knnq.getKNN(cur, kplus);
    for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
      // skip the point itself
      if(DBIDUtil.equal(cur, neighbor)) {
        continue;
      }
      sum += lrds.doubleValue(neighbor);
      ++count;
    }
    return sum / (lrdp * count);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOF score, must be an integer greater than
     * or equal to 1.
     */
    public static final OptionID K_ID = new OptionID("lof.k", "The number of nearest neighbors (not including the query point) of an object to be considered for computing its LOF score.");

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
    }

    @Override
    public LOF<O> make() {
      return new LOF<>(k, distance);
    }
  }
}
