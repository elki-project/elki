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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * <p>
 * Outlier Detection based on the distance of an object to its k nearest
 * neighbor.
 * </p>
 * 
 * <p>
 * Reference:<br>
 * S. Ramaswamy, R. Rastogi, K. Shim: Efficient Algorithms for Mining Outliers
 * from Large Data Sets.</br> In: Proc. of the Int. Conf. on Management of Data,
 * Dallas, Texas, 2000.
 * </p>
 * 
 * @author Lisa Reichert
 * 
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
@Title("KNN outlier: Efficient Algorithms for Mining Outliers from Large Data Sets")
@Description("Outlier Detection based on the distance of an object to its k nearest neighbor.")
@Reference(authors = "S. Ramaswamy, R. Rastogi, K. Shim", title = "Efficient Algorithms for Mining Outliers from Large Data Sets", booktitle = "Proc. of the Int. Conf. on Management of Data, Dallas, Texas, 2000", url = "http://dx.doi.org/10.1145/342009.335437")
public class KNNOutlier<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(KNNOutlier.class);

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knno.k", "k nearest neighbor");

  /**
   * The parameter k
   */
  private int k;

  /**
   * Constructor for a single kNN query.
   * 
   * @param distanceFunction distance function to use
   * @param k Value of k
   */
  public KNNOutlier(DistanceFunction<? super O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    final DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distanceQuery, k);

    if(logger.isVerbose()) {
      logger.verbose("Computing the kNN outlier degree (distance to the k nearest neighbor)");
    }
    FiniteProgress progressKNNDistance = logger.isVerbose() ? new FiniteProgress("kNN distance for objects", relation.size(), logger) : null;

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore knno_score = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    // compute distance to the k nearest neighbor.
    for(DBID id : relation.iterDBIDs()) {
      // distance to the kth nearest neighbor
      final KNNResult<D> knns = knnQuery.getKNNForDBID(id, k);
      double dkn = knns.getKNNDistance().doubleValue();
      knno_score.putDouble(id, dkn);

      minmax.put(dkn);

      if(progressKNNDistance != null) {
        progressKNNDistance.incrementProcessed(logger);
      }
    }
    if(progressKNNDistance != null) {
      progressKNNDistance.ensureCompleted(logger);
    }
    Relation<Double> scoreres = new MaterializedRelation<Double>("kNN Outlier Score", "knn-outlier", TypeUtil.DOUBLE, knno_score, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected KNNOutlier<O, D> makeInstance() {
      return new KNNOutlier<O, D>(distanceFunction, k);
    }
  }
}