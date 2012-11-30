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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
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
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A simplified version of the original LOF algorithm, which does not use the
 * reachability distance, yielding less stable results on inliers.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
public class SimpleLOF<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimpleLOF.class);

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  private static boolean objectIsInKNN = false;

  /**
   * Parameter k.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param k the value of k
   */
  public SimpleLOF(int k, DistanceFunction<? super O, D> distance) {
    super(distance);
    this.k = k + (objectIsInKNN ? 0 : 1);
  }

  /**
   * Run the Simple LOF algorithm.
   * 
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("SimpleLOF", 3) : null;

    DBIDs ids = relation.getDBIDs();

    // "HEAVY" flag for KNN Query since it is used more than once
    KNNQuery<O, D> knnq = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if (!(knnq instanceof PreprocessorKNNQuery)) {
      if (stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhoods w.r.t. distance function.", LOG);
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<O, D>(relation, getDistanceFunction(), k);
      relation.getDatabase().addIndex(preproc);
      DistanceQuery<O, D> rdq = relation.getDatabase().getDistanceQuery(relation, getDistanceFunction());
      knnq = preproc.getKNNQuery(rdq, k);
    }

    // Compute LRDs
    if (stepprog != null) {
      stepprog.beginStep(2, "Computing densities.", LOG);
    }
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for (DBIDIter iter1 = ids.iter(); iter1.valid(); iter1.advance()) {
      final KNNResult<D> neighbors1 = knnq.getKNNForDBID(iter1, k);
      double sum1 = 0.0;
      int count1 = 0;
      if (neighbors1 instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for (DoubleDistanceDBIDResultIter neighbor2 = ((DoubleDistanceKNNList) neighbors1).iter(); neighbor2.valid(); neighbor2.advance()) {
          if (objectIsInKNN || !DBIDUtil.equal(neighbor2, iter1)) {
            sum1 += neighbor2.doubleDistance();
            count1++;
          }
        }
      } else {
        for (DistanceDBIDResultIter<D> neighbor1 = neighbors1.iter(); neighbor1.valid(); neighbor1.advance()) {
          if (objectIsInKNN || !DBIDUtil.equal(neighbor1, iter1)) {
            sum1 += neighbor1.getDistance().doubleValue();
            count1++;
          }
        }
      }
      // Avoid division by 0
      final double lrd = (sum1 > 0) ? (count1 / sum1) : 0;
      dens.putDouble(iter1, lrd);
      if (densProgress != null) {
        densProgress.incrementProcessed(LOG);
      }
    }
    if (densProgress != null) {
      densProgress.ensureCompleted(LOG);
    }

    // compute LOF_SCORE of each db object
    if (stepprog != null) {
      stepprog.beginStep(3, "Computing SLOFs.", LOG);
    }
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("LOF_SCORE for objects", ids.size(), LOG) : null;
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lrdp = dens.doubleValue(iter);
      final double lof;
      if (lrdp > 0) {
        final KNNResult<D> neighbors = knnq.getKNNForDBID(iter, k);
        final int size = neighbors.size() - 1; // estimated
        double sum = 0.0;
        int count = 0;
        for (DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if (objectIsInKNN || !DBIDUtil.equal(neighbor, iter)) {
            sum += dens.doubleValue(neighbor) / size;
            count++;
          }
        }
        if (count == size) {
          lof = sum / lrdp;
        } else {
          // Correct estimation
          sum *= size / (double) count;
          lof = sum / lrdp;
        }
      } else {
        lof = 1.0;
      }
      lofs.putDouble(iter, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      if (progressLOFs != null) {
        progressLOFs.incrementProcessed(LOG);
      }
    }
    if (progressLOFs != null) {
      progressLOFs.ensureCompleted(LOG);
    }

    if (stepprog != null) {
      stepprog.setCompleted(LOG);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<Double>("Simple Local Outlier Factor", "simple-lof-outlier", TypeUtil.DOUBLE, lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return result;
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
     * The neighborhood size to use.
     */
    protected int k = 2;

    /**
     * Neighborhood distance function.
     */
    protected DistanceFunction<O, D> neighborhoodDistanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(LOF.K_ID);
      pK.addConstraint(new GreaterConstraint(1));
      if (config.grab(pK)) {
        k = pK.getValue();
      }
    }

    @Override
    protected SimpleLOF<O, D> makeInstance() {
      return new SimpleLOF<O, D>(k, distanceFunction);
    }
  }
}
