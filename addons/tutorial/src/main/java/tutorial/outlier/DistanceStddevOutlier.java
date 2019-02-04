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
package tutorial.outlier;

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
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * A simple outlier detection algorithm that computes the standard deviation of
 * the kNN distances.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type
 */
public class DistanceStddevOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(DistanceStddevOutlier.class);

  /**
   * Number of neighbors to get.
   */
  protected int k;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function to use
   * @param k Number of neighbors to use
   */
  public DistanceStddevOutlier(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Run the outlier detection algorithm
   *
   * @param database Database to use
   * @param relation Relation to analyze
   * @return Outlier score result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    // Get a nearest neighbor query on the relation.
    KNNQuery<O> knnq = QueryUtil.getKNNQuery(relation, getDistanceFunction(), k);
    // Output data storage
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    // Track minimum and maximum scores
    DoubleMinMax minmax = new DoubleMinMax();

    // Iterate over all objects
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      KNNList neighbors = knnq.getKNNForDBID(iter, k);
      // Aggregate distances
      MeanVariance mv = new MeanVariance();
      for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
        // Skip the object itself. The 0 is not very informative.
        if(DBIDUtil.equal(iter, neighbor)) {
          continue;
        }
        mv.put(neighbor.doubleValue());
      }
      // Store score
      scores.putDouble(iter, mv.getSampleStddev());
    }

    // Wrap the result in the standard containers
    // Actual min-max, theoretical min-max!
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0, Double.POSITIVE_INFINITY);
    DoubleRelation rel = new MaterializedDoubleRelation(relation.getDBIDs(), "stddev-outlier", scores);
    return new OutlierResult(meta, rel);
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
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID for parameterization.
     */
    public static final OptionID K_ID = new OptionID("stddevout.k", "Number of neighbors to get for stddev based outlier detection.");

    /**
     * Number of neighbors to get
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      // The super class has the distance function parameter!
      super.makeOptions(config);
      IntParameter kParam = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(kParam)) {
        k = kParam.getValue();
      }
    }

    @Override
    protected DistanceStddevOutlier<O> makeInstance() {
      return new DistanceStddevOutlier<>(distanceFunction, k);
    }
  }
}