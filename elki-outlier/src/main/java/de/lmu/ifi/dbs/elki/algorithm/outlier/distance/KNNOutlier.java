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
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Outlier Detection based on the distance of an object to its k nearest
 * neighbor.
 * <p>
 * This implementation differs from the original pseudocode: the k nearest
 * neighbors do not exclude the point that is currently evaluated. I.e. for k=1
 * the resulting score is the distance to the 1-nearest neighbor that is not the
 * query point and therefore should match k=2 in the exact pseudocode - a value
 * of k=1 in the original code does not make sense, as the 1NN distance will be
 * 0 for every point in the database. If you for any reason want to use the
 * original algorithm, subtract 1 from the k parameter.
 * <p>
 * Reference:
 * <p>
 * S. Ramaswamy, R. Rastogi, K. Shim<br>
 * Efficient Algorithms for Mining Outliers from Large Data Sets.<br>
 * In: Proc. Int. Conf. on Management of Data (SIGMOD 2000)
 *
 * @author Lisa Reichert
 * @since 0.3
 *
 * @has - - - KNNQuery
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", //
    title = "Efficient Algorithms for Mining Outliers from Large Data Sets", //
    booktitle = "Proc. Int. Conf. on Management of Data (SIGMOD 2000)", //
    url = "https://doi.org/10.1145/342009.335437", //
    bibkey = "DBLP:conf/sigmod/RamaswamyRS00")
@Alias({ "de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier", "knno" })
@Priority(Priority.RECOMMENDED)
public class KNNOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNOutlier.class);

  /**
   * The parameter k (including query point!)
   */
  private int k;

  /**
   * Constructor for a single kNN query.
   *
   * @param distanceFunction distance function to use
   * @param k Value of k (excluding query point!)
   */
  public KNNOutlier(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k + 1; // INCLUDE the query point now
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param database Database (no longer used)
   * @param relation Data relation
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    return run(relation);
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   *
   * @param relation Data relation
   */
  public OutlierResult run(Relation<O> relation) {
    final DistanceQuery<O> distanceQuery = relation.getDistanceQuery(getDistanceFunction());
    final KNNQuery<O> knnQuery = relation.getKNNQuery(distanceQuery, k);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), LOG) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore knno_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    // compute distance to the k nearest neighbor.
    for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
      // distance to the kth nearest neighbor
      // (assuming the query point is always included, with distance 0)
      final double dkn = knnQuery.getKNNForDBID(it, k).getKNNDistance();

      knno_score.putDouble(it, dkn);
      minmax.put(dkn);

      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    DoubleRelation scoreres = new MaterializedDoubleRelation("kNN Outlier Score", "knn-outlier", knno_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(meta, scoreres);
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
     * Parameter to specify the k nearest neighbor
     */
    public static final OptionID K_ID = new OptionID("knno.k", //
        "The k nearest neighbor, excluding the query point "//
            + "(i.e. query point is the 0-nearest-neighbor)");

    /**
     * k parameter
     */
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNOutlier<O> makeInstance() {
      return new KNNOutlier<>(distanceFunction, k);
    }
  }
}
