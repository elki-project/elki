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
package elki.outlier.distance;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.outlier.OutlierAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DoubleDataStore;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.outlier.ProbabilisticOutlierScore;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Simple distance based outlier detection algorithms.
 * <p>
 * Reference:
 * <p>
 * E.M. Knorr, R. T. Ng:<br>
 * Algorithms for Mining Distance-Based Outliers in Large Datasets,<br>
 * In: Proc. Int. Conf. on Very Large Databases (VLDB'98)
 * 
 * @author Lisa Reichert
 * @since 0.3
 * 
 * @param <O> the type of objects handled by this algorithm
 */
@Reference(authors = "E. M. Knorr, R. T. Ng", //
    title = "Algorithms for Mining Distance-Based Outliers in Large Datasets", //
    booktitle = "Proc. Int. Conf. on Very Large Databases (VLDB'98)", //
    url = "http://www.vldb.org/conf/1998/p392.pdf", //
    bibkey = "DBLP:conf/vldb/KnorrN98")
public abstract class AbstractDBOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Radius parameter d.
   */
  private double d;

  /**
   * Constructor with actual parameters.
   * 
   * @param distanceFunction distance function to use
   * @param d radius d value
   */
  public AbstractDBOutlier(Distance<? super O> distanceFunction, double d) {
    super(distanceFunction);
    this.d = d;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   * @param database Database to process
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    // Run the actual score process
    DoubleDataStore dbodscore = computeOutlierScores(database, relation, d);

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Density-Based Outlier Detection", relation.getDBIDs(), dbodscore);
    OutlierScoreMeta scoreMeta = new ProbabilisticOutlierScore();
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * computes an outlier score for each object of the database.
   * 
   * @param database Database
   * @param relation Relation
   * @param d distance
   * @return computed scores
   */
  protected abstract DoubleDataStore computeOutlierScores(Database database, Relation<O> relation, double d);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the size of the D-neighborhood
     */
    public static final OptionID D_ID = new OptionID("dbod.d", "size of the D-neighborhood");

    /**
     * Query radius
     */
    protected double d;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configD(config, distanceFunction);
    }

    /**
     * Grab the 'd' configuration option.
     * 
     * @param config Parameterization
     */
    protected void configD(Parameterization config, Distance<?> distanceFunction) {
      final DoubleParameter param = new DoubleParameter(D_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(param)) {
        d = param.getValue();
      }
    }
  }
}
