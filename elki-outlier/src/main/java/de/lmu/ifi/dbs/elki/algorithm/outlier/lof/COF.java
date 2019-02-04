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
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Connectivity-based Outlier Factor (COF).
 * <p>
 * Reference:
 * <p>
 * J. Tang, Z. Chen, A. W. C. Fu, D. W. Cheung<br>
 * Enhancing effectiveness of outlier detections for low density patterns.<br>
 * Advances in Knowledge Discovery and Data Mining.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
@Title("COF: Connectivity-based Outlier Factor")
@Reference(authors = "J. Tang, Z. Chen, A. W. C. Fu, D. W. Cheung", //
    title = "Enhancing effectiveness of outlier detections for low density patterns", //
    booktitle = "In Advances in Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1007/3-540-47887-6_53", //
    bibkey = "DBLP:conf/pakdd/TangCFC02")
public class COF<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COF.class);

  /**
   * The number of neighbors to query (including the query point!)
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param k the number of neighbors to use for comparison (excluding the query
   *        point)
   * @param distanceFunction the neighborhood distance function
   */
  public COF(int k, DistanceFunction<? super O> distanceFunction) {
    super(distanceFunction);
    this.k = k + 1; // + query point
  }

  /**
   * Runs the COF algorithm on the given database.
   *
   * @param database Database to query
   * @param relation Data to process
   * @return COF outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    StepProgress stepprog = LOG.isVerbose() ? new StepProgress("COF", 3) : null;
    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    LOG.beginStep(stepprog, 1, "Materializing COF neighborhoods.");
    KNNQuery<O> knnq = DatabaseUtil.precomputedKNNQuery(database, relation, dq, k);
    DBIDs ids = relation.getDBIDs();

    LOG.beginStep(stepprog, 2, "Computing Average Chaining Distances.");
    WritableDoubleDataStore acds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    computeAverageChainingDistances(knnq, dq, ids, acds);

    // compute COF_SCORE of each db object
    LOG.beginStep(stepprog, 3, "Computing Connectivity-based Outlier Factors.");
    WritableDoubleDataStore cofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    // track the maximum value for normalization.
    DoubleMinMax cofminmax = new DoubleMinMax();
    computeCOFScores(knnq, ids, acds, cofs, cofminmax);

    LOG.setCompleted(stepprog);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Connectivity-Based Outlier Factor", "cof-outlier", cofs, ids);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(cofminmax.getMin(), cofminmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Computes the average chaining distance, the average length of a path
   * through the given set of points to each target. The authors of COF decided
   * to approximate this value using a weighted mean that assumes every object
   * is reached from the previous point (but actually every point could be best
   * reachable from the first, in which case this does not make much sense.)
   *
   * TODO: can we accelerate this by using the kNN of the neighbors?
   *
   * @param knnq KNN query
   * @param dq Distance query
   * @param ids IDs to process
   * @param acds Storage for average chaining distances
   */
  protected void computeAverageChainingDistances(KNNQuery<O> knnq, DistanceQuery<O> dq, DBIDs ids, WritableDoubleDataStore acds) {
    FiniteProgress lrdsProgress = LOG.isVerbose() ? new FiniteProgress("Computing average chaining distances", ids.size(), LOG) : null;

    // Compute the chaining distances.
    // We do <i>not</i> bother to materialize the chaining order.
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      final int r = neighbors.size();
      DoubleDBIDListIter it1 = neighbors.iter(), it2 = neighbors.iter();
      // Store the current lowest reachability.
      final double[] mindists = new double[r];
      for(int i = 0; it1.valid(); it1.advance(), ++i) {
        mindists[i] = DBIDUtil.equal(it1, iter) ? Double.NaN : it1.doubleValue();
      }

      double acsum = 0.;
      for(int j = ((r < k) ? r : k) - 1; j > 0; --j) {
        // Find the minimum:
        int minpos = -1;
        double mindist = Double.NaN;
        for(int i = 0; i < mindists.length; ++i) {
          double curdist = mindists[i];
          // Both values could be NaN, deliberately.
          if(curdist == curdist && !(curdist > mindist)) {
            minpos = i;
            mindist = curdist;
          }
        }
        acsum += mindist * j; // Weighted sum, decreasing weights
        mindists[minpos] = Double.NaN;
        it1.seek(minpos);
        // Update distances
        it2.seek(0);
        for(int i = 0; it2.valid(); it2.advance(), ++i) {
          final double curdist = mindists[i];
          if(curdist != curdist) {
            continue; // NaN = processed!
          }
          double newdist = dq.distance(it1, it2);
          if(newdist < curdist) {
            mindists[i] = newdist;
          }
        }
      }
      acds.putDouble(iter, acsum / (r * 0.5 * (r - 1.)));
      LOG.incrementProcessed(lrdsProgress);
    }
    LOG.ensureCompleted(lrdsProgress);
  }

  /**
   * Compute Connectivity outlier factors.
   *
   * @param knnq KNN query
   * @param ids IDs to process
   * @param acds Average chaining distances
   * @param cofs Connectivity outlier factor storage
   * @param cofminmax Score minimum/maximum tracker
   */
  private void computeCOFScores(KNNQuery<O> knnq, DBIDs ids, DoubleDataStore acds, WritableDoubleDataStore cofs, DoubleMinMax cofminmax) {
    FiniteProgress progressCOFs = LOG.isVerbose() ? new FiniteProgress("COF for objects", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      final KNNList neighbors = knnq.getKNNForDBID(iter, k);
      // Aggregate the average chaining distances of all neighbors:
      double sum = 0.;
      for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        // skip the point itself
        if(DBIDUtil.equal(neighbor, iter)) {
          continue;
        }
        sum += acds.doubleValue(neighbor);
      }
      final double cof = (sum > 0.) ? (acds.doubleValue(iter) * k / sum) : (acds.doubleValue(iter) > 0. ? Double.POSITIVE_INFINITY : 1.);
      cofs.putDouble(iter, cof);
      // update minimum and maximum
      cofminmax.put(cof);

      LOG.incrementProcessed(progressCOFs);
    }
    LOG.ensureCompleted(progressCOFs);
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
     * Parameter to specify the neighborhood size for COF. This does not include
     * the query object.
     */
    public static final OptionID K_ID = new OptionID("cof.k", "The number of neighbors (not including the query object) to use for computing the COF score.");

    /**
     * The neighborhood size to use.
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      final IntParameter pK = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(pK)) {
        k = pK.intValue();
      }
    }

    @Override
    protected COF<O> makeInstance() {
      return new COF<>(k, distanceFunction);
    }
  }
}
