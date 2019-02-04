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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Local Outlier Detection Reconsidered: a Generalized View on Locality with
 * Applications to Spatial, Video, and Network Outlier Detection<br>
 * Data Mining and Knowledge Discovery 28(1)
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of data objects handled by this algorithm
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Local Outlier Detection Reconsidered: a Generalized View on Locality with Applications to Spatial, Video, and Network Outlier Detection", //
    booktitle = "Data Mining and Knowledge Discovery 28(1)", //
    url = "https://doi.org/10.1007/s10618-012-0300-z", //
    bibkey = "DBLP:journals/datamine/SchubertZK14")
@Alias("de.lmu.ifi.dbs.elki.algorithm.outlier.SimpleLOF")
public class SimplifiedLOF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SimplifiedLOF.class);

  /**
   * The number of neighbors to query, excluding the query point.
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param k the value of k
   */
  public SimplifiedLOF(int k, DistanceFunction<? super O> distance) {
    super(distance);
    this.k = k + 1; // + query point
  }

  /**
   * Run the Simple LOF algorithm.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return LOF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("Simplified LOF", 3) : null;
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 1, "Materializing neighborhoods w.r.t. distance function.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, getDistanceFunction(), k);

    // Compute LRDs
    LOG.beginStep(stepprog, 2, "Computing densities.");
    WritableDoubleDataStore dens = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeSimplifiedLRDs(ids, knnq, dens);

    // compute LOF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing SLOFs.");
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_STATIC);
    DoubleMinMax lofminmax = new DoubleMinMax();
    computeSimplifiedLOFs(ids, knnq, dens, lofs, lofminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Simplified Local Outlier Factor", "simplified-lof-outlier", lofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(lofminmax.getMin(), lofminmax.getMax(), 0., Double.POSITIVE_INFINITY, 1.);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);

    return result;
  }

  /**
   * Compute the simplified reachability densities.
   *
   * @param ids IDs to process
   * @param knnq kNN query class
   * @param lrds Density output
   */
  private void computeSimplifiedLRDs(DBIDs ids, KNNQuery<O> knnq, WritableDoubleDataStore lrds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("Densities", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      double sum = 0.0;
      int count = 0;
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(neighbor, iter)) {
          continue;
        }
        sum += neighbor.doubleValue();
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
   * Compute the simplified LOF factors.
   *
   * @param ids IDs to compute for
   * @param knnq kNN query class
   * @param slrds Object densities
   * @param lofs SLOF output storage
   * @param lofminmax Minimum and maximum scores
   */
  private void computeSimplifiedLOFs(DBIDs ids, KNNQuery<O> knnq, WritableDoubleDataStore slrds, WritableDoubleDataStore lofs, DoubleMinMax lofminmax) {
    FiniteProgress progressLOFs = LOG.isVerbose() ? new FiniteProgress("Simplified LOF scores", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final double lof;
      final double lrdp = slrds.doubleValue(iter);
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      if(!Double.isInfinite(lrdp)) {
        double sum = 0.;
        int count = 0;
        for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          // skip the point itself
          if(DBIDUtil.equal(neighbor, iter)) {
            continue;
          }
          final double val = slrds.doubleValue(neighbor);
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
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * The neighborhood size to use.
     */
    protected int k = 2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(LOF.Parameterizer.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK)) {
        k = pK.getValue();
      }
    }

    @Override
    protected SimplifiedLOF<O> makeInstance() {
      return new SimplifiedLOF<>(k, distanceFunction);
    }
  }
}
