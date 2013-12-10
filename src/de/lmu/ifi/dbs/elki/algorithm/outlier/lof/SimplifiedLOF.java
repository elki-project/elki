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
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A simplified version of the original LOF algorithm, which does not use the
 * reachability distance, yielding less stable results on inliers.
 * 
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br />
 * Local outlier detection reconsidered: a generalized view on locality with
 * applications to spatial, video, and network outlier detection<br />
 * In: Data Mining and Knowledge Discovery
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> Distance type
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", title = "Local outlier detection reconsidered: a generalized view on locality with applications to spatial, video, and network outlier detection", booktitle = "Data Mining and Knowledge Discovery", url = "http://dx.doi.org/10.1007/s10618-012-0300-z")
@Alias({ "SimpleLOF", "outlier.SimpleLOF", "de.lmu.ifi.dbs.elki.algorithm.outlier.SimpleLOF" })
public class SimplifiedLOF<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimplifiedLOF.class);

  /**
   * Parameter k.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param k the value of k
   */
  public SimplifiedLOF(int k, DistanceFunction<? super O, D> distance) {
    super(distance);
    this.k = k + 1;
  }

  /**
   * Run the Simple LOF algorithm.
   * 
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("SimpleLOF", 3) : null;

    DBIDs ids = relation.getDBIDs();

    // "HEAVY" flag for KNN Query since it is used more than once
    KNNQuery<O, D> knnq = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY, DatabaseQuery.HINT_NO_CACHE);
    // No optimized kNN query - use a preprocessor!
    if(!(knnq instanceof PreprocessorKNNQuery)) {
      if(stepprog != null) {
        stepprog.beginStep(1, "Materializing neighborhoods w.r.t. distance function.", LOG);
      }
      MaterializeKNNPreprocessor<O, D> preproc = new MaterializeKNNPreprocessor<>(relation, getDistanceFunction(), k);
      database.addIndex(preproc);
      DistanceQuery<O, D> rdq = database.getDistanceQuery(relation, getDistanceFunction());
      knnq = preproc.getKNNQuery(rdq, k);
    }

    // Compute LRDs
    if(stepprog != null) {
      stepprog.beginStep(2, "Computing densities.", LOG);
    }
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    FiniteProgress densProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final KNNList<D> neighbors = knnq.getKNNForDBID(it, k);
      double sum = 0.0;
      int count = 0;
      if(neighbors instanceof DoubleDistanceKNNList) {
        // Fast version for double distances
        for(DoubleDistanceDBIDListIter neighbor = ((DoubleDistanceKNNList) neighbors).iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          sum += neighbor.doubleDistance();
          count++;
        }
      }
      else {
        for(DistanceDBIDListIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          sum += neighbor.getDistance().doubleValue();
          count++;
        }
      }
      // Avoid division by 0
      final double lrd = (sum > 0) ? (count / sum) : 0;
      dens.putDouble(it, lrd);
      if(densProgress != null) {
        densProgress.incrementProcessed(LOG);
      }
    }
    if(densProgress != null) {
      densProgress.ensureCompleted(LOG);
    }

    // compute LOF_SCORE of each db object
    if(stepprog != null) {
      stepprog.beginStep(3, "Computing SLOFs.", LOG);
    }
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    // track the maximum value for normalization.
    DoubleMinMax lofminmax = new DoubleMinMax();

    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("Simple LOF scores.", ids.size(), LOG) : null;
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      final double lrdp = dens.doubleValue(it);
      final double lof;
      if(lrdp > 0) {
        final KNNList<D> neighbors = knnq.getKNNForDBID(it, k);
        double sum = 0.0;
        int count = 0;
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if(DBIDUtil.equal(neighbor, it)) {
            continue;
          }
          sum += dens.doubleValue(neighbor);
          count++;
        }
        lof = sum / (count * lrdp);
      }
      else {
        lof = 1.0;
      }
      lofs.putDouble(it, lof);
      // update minimum and maximum
      lofminmax.put(lof);

      if(progressLOFs != null) {
        progressLOFs.incrementProcessed(LOG);
      }
    }
    if(progressLOFs != null) {
      progressLOFs.ensureCompleted(LOG);
    }

    if(stepprog != null) {
      stepprog.setCompleted(LOG);
    }

    // Build result representation.
    Relation<Double> scoreResult = new MaterializedRelation<>("Simple Local Outlier Factor", "simple-lof-outlier", TypeUtil.DOUBLE, lofs, ids);
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
   * 
   * @param <O> vector type
   * @param <D> distance type
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(LOF.Parameterizer.K_ID);
      pK.addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }
    }

    @Override
    protected SimplifiedLOF<O, D> makeInstance() {
      return new SimplifiedLOF<>(k, distanceFunction);
    }
  }
}
