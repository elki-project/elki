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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier Detection based on the accumulated distances of a point to its k
 * nearest neighbors.
 * <p>
 * As in the original publication (as far as we could tell from the pseudocode
 * included), the current point is not included in the nearest neighbors (see
 * figures in the publication). This matches the intuition common in nearest
 * neighbor classification, where the evaluated instances are not part of the
 * training set; but it contrasts to the pseudocode of the kNN outlier method
 * and the database interpretation (which returns all objects stored in the
 * database).
 * <p>
 * Furthermore, we report the sum of the k distances (called "weight" in the
 * original publication). Other implementations may return the average distance
 * instead, and therefore yield different results.
 * <p>
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti<br>
 * Fast Outlier Detection in High Dimensional Spaces<br>
 * Proc. European Conf. Principles of Knowledge Discovery and Data Mining
 * (PKDD'02)
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("KNNWeight outlier detection")
@Description("Outlier detection based on the sum of distances of an object to its k nearest neighbors.")
@Reference(authors = "F. Angiulli, C. Pizzuti", //
    title = "Fast Outlier Detection in High Dimensional Spaces", //
    booktitle = "Proc. European Conf. Principles of Knowledge Discovery and Data Mining (PKDD'02)", //
    url = "https://doi.org/10.1007/3-540-45681-3_2", //
    bibkey = "DBLP:conf/pkdd/AngiulliP02")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.KNNWeightOutlier", "knnw" })
public class KNNWeightOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNWeightOutlier.class);

  /**
   * Holds the number of nearest neighbors to query (excluding the query point!)
   */
  private int k;

  /**
   * Constructor with parameters.
   *
   * @param distanceFunction Distance function
   * @param k k Parameter (not including query point!)
   */
  public KNNWeightOutlier(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param database Database context
   * @param relation Data relation
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnQuery = database.getKNNQuery(distanceQuery, k + 1); // +
                                                                       // query
                                                                       // point

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Compute kNN weights", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore knnw_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final KNNList knn = knnQuery.getKNNForDBID(iditer, k + 1); // + query
                                                                 // point
      double skn = 0; // sum of the distances to the k nearest neighbors
      int i = 0; // number of neighbors so far
      for(DoubleDBIDListIter neighbor = knn.iter(); i < k && neighbor.valid(); neighbor.advance()) {
        if(DBIDUtil.equal(iditer, neighbor)) {
          continue;
        }
        skn += neighbor.doubleValue();
        ++i;
      }
      if(i < k) {
        // Less than k neighbors found
        // Approximative index, or k > data set size!
        skn = Double.POSITIVE_INFINITY;
      }
      knnw_score.putDouble(iditer, skn);
      minmax.put(skn);

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    DoubleRelation res = new MaterializedDoubleRelation("kNN weight Outlier Score", "knnw-outlier", knnw_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(meta, res);
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
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the k nearest neighbor.
     */
    public static final OptionID K_ID = new OptionID("knnwod.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

    /**
     * k parameter
     */
    protected int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNWeightOutlier<O> makeInstance() {
      return new KNNWeightOutlier<>(distanceFunction, k);
    }
  }
}
