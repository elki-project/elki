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
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceKNNList;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
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
 * <p>
 * Reference: <br>
 * M. M. Breunig, H.-P. Kriegel, R. Ng, J. Sander: LOF: Identifying
 * Density-Based Local Outliers. <br>
 * In: Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'00),
 * Dallas, TX, 2000.
 * </p>
 * 
 * @author Erich Schubert
 * @author Elke Achtert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Title("LOF: Local Outlier Factor")
@Description("Algorithm to compute density-based local outlier factors in a database based on the neighborhood size parameter 'k'")
@Reference(authors = "M. M. Breunig, H.-P. Kriegel, R. Ng, and J. Sander", title = "LOF: Identifying Density-Based Local Outliers", booktitle = "Proc. 2nd ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '00), Dallas, TX, 2000", url = "http://dx.doi.org/10.1145/342009.335388")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.LOF", "outlier.LOF", "LOF" })
public class LOF<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(LOF.class);

  /**
   * Holds the value of {@link Parameterizer#K_ID}.
   */
  protected int k = 2;

  /**
   * Constructor.
   * 
   * @param k the value of k
   * @param distanceFunction the neighborhood distance function
   */
  public LOF(int k, DistanceFunction<? super O, D> distanceFunction) {
    super(distanceFunction);
    this.k = k + 1;
  }

  /**
   * Performs the Generalized LOF_SCORE algorithm on the given database.
   * 
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("LOF", 3) : null;
    DistanceQuery<O, D> dq = database.getDistanceQuery(relation, getDistanceFunction());
    // "HEAVY" flag for knn query since it is used more than once
    KNNQuery<O, D> knnq = database.getKNNQuery(dq, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing LOF neighborhoods.", LOG);
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(relation, getDistanceFunction(), k);
      knnq = preproc.getKNNQuery(dq, k);
    }
    DBIDs ids = relation.getDBIDs();

    // Compute LRDs
    if(stepprog != null) {
      stepprog.beginStep(2, "Computing LRDs.", LOG);
    }
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeLRDs(knnq, ids, lrds);

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing LOFs.", LOG);
    }
    DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();
    computeLOFScores(knnq, ids, lrds, lofs, lofminmax);

    if(stepprog != null) {
      stepprog.setCompleted(LOG);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Local Outlier Factor", "lof-outlier", TypeUtil.DOUBLE, lofs, ids);
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
  private void computeLRDs(KNNQuery<O, D> knnq, DBIDs ids, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("LRD", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList<D> neighbors = knnq.getKNNForDBID(iter, k);
      double sum = 0.0;
      int count = 0;
      if(neighbors instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for(DoubleDistanceDBIDListIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, iter)) {
            continue;
          }
          KNNList<D> neighborsNeighbors = knnq.getKNNForDBID(neighbor, k);
          final double nkdist;
          if(neighborsNeighbors instanceof DoubleDistanceKNNList) {
            nkdist = ((DoubleDistanceKNNList) neighborsNeighbors).doubleKNNDistance();
          }
          else {
            nkdist = neighborsNeighbors.getKNNDistance().doubleValue();
          }
          sum += Math.max(neighbor.doubleDistance(), nkdist);
          count++;
        }
      }
      else {
        for(DistanceDBIDListIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, iter)) {
            continue;
          }
          KNNList<D> neighborsNeighbors = knnq.getKNNForDBID(neighbor, k);
          sum += Math.max(neighbor.getDistance().doubleValue(), neighborsNeighbors.getKNNDistance().doubleValue());
          count++;
        }
      }
      // Avoid division by 0
      final double lrd = (sum > 0) ? (count / sum) : Double.POSITIVE_INFINITY;
      lrds.putDouble(iter, lrd);
      if(lrdsProgress != null) {
        lrdsProgress.incrementProcessed(LOG);
      }
    }
    if(lrdsProgress != null) {
      lrdsProgress.ensureCompleted(LOG);
    }
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
  private void computeLOFScores(KNNQuery<O, D> knnq, DBIDs ids, DoubleDataStore lrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lof;
      final double lrdp = lrds.doubleValue(iter);
      final KNNList<D> neighbors = knnq.getKNNForDBID(iter, k);
      if(!Double.isInfinite(lrdp)) {
        double sum = 0.0;
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

      if(progressLOFs != null) {
        progressLOFs.incrementProcessed(LOG);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(LOG);
    }
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
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * Parameter to specify the number of nearest neighbors of an object to be
     * considered for computing its LOF_SCORE, must be an integer greater than
     * 1.
     */
    public static final OptionID K_ID = new OptionID("lof.k", "The number of nearest neighbors of an object to be considered for computing its LOF_SCORE.");

    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID);
      pK.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }
    }

    @Override
    protected LOF<O, D> makeInstance() {
      return new LOF<>(k, distanceFunction);
    }
  }
}
