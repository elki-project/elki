package de.lmu.ifi.dbs.elki.algorithm.outlier.lof;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Algorithm to compute density-based local outlier factors in a database based
 * on a specified parameter {@link Parameterizer#K_ID} ({@code -lof.k}).
 * </p>
 * 
 * <p>
 * The original LOF parameter was called &quot;minPts&quot;, but for consistency
 * within ELKI we have renamed this parameter to &quot;k&quot;.
 * </p>
 * 
 * Reference:
 * <p>
 * M. M. Breunig, H.-P. Kriegel, R. Ng, J. Sander:<br />
 * LOF: Identifying Density-Based Local Outliers.<br />
 * In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00),
 * Dallas, TX, 2000.
 * </p>
 * 
 * @author Erich Schubert
 * @author Elke Achtert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of data objects handled by this algorithm
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander",//
title = "LOF: Identifying Density-Based Local Outliers", //
booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", //
url = "http://dx.doi.org/10.1145/342009.335388")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.LOF", "outlier.LOF", "LOF" })
public class LOF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LOF.class);

  /**
   * The number of neighbors to query (including the query point!)
   */
  protected int k = 2;

  /**
   * Constructor.
   * 
   * @param k the number of neighbors to use for comparison (excluding the query
   *        point)
   * @param distanceFunction the neighborhood distance function
   */
  public LOF(int k, DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
    this.k = k + 1;
  }

  /**
   * Runs the LOF algorithm on the given database.
   * 
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LOF", 3) : null;
    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    // "HEAVY" flag for knn query since it is used more than once
    KNNQuery<O> knnq = database.getKNNQuery(dq, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      LOG.beginStep(stepprog, 1, "Materializing LOF neighborhoods.");
      MaterializeKNNPreprocessor<O> preproc = new MaterializeKNNPreprocessor<>(relation, getDistanceFunction(), k);
      knnq = preproc.getKNNQuery(dq, k);
    }
    DBIDs ids = relation.getDBIDs();

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing LRDs.");
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeLRDs(knnq, ids, lrds);

    // compute LOF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing LOFs.");
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();
    computeLOFScores(knnq, ids, lrds, lofs, lofminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Local Outlier Factor", "lof-outlier", lofs, ids);
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
  private void computeLRDs(KNNQuery<O> knnq, DBIDs ids, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("LRD", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      double sum = 0.0;
      int count = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iter)) {
          continue;
        }
        KNNList neighborsNeighbors = knnq.getKNNForDBID(neighbor, k);
        sum += Math.max(neighbor.doubleValue(), neighborsNeighbors.getKNNDistance());
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
   * Compute local outlier factors.
   * 
   * @param knnq KNN query
   * @param ids IDs to process
   * @param lrds Local reachability distances
   * @param lofs Local outlier factor storage
   * @param lofminmax Score minimum/maximum tracker
   */
  private void computeLOFScores(KNNQuery<O> knnq, DBIDs ids, DoubleDataStore lrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lof;
      final double lrdp = lrds.doubleValue(iter);
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
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
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOF score, must be an integer greater than
     * or equal to 1.
     */
    public static final OptionID K_ID = new OptionID("lof.k", "The number of nearest neighbors of an object to be considered for computing its LOF score.");

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID);
      pK.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK)) {
        k = pK.intValue();
      }
    }

    @Override
    protected LOF<O> makeInstance() {
      return new LOF<>(k, distanceFunction);
    }
  }
}
