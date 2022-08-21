/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics.intrinsicdimensionality;

import elki.database.ids.DBIDRef;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Angle based intrinsic dimensionality (ABID) estimator.
 * <p>
 * Reference:
 * <p>
 * Erik Thordsen and Erich Schubert<br>
 * ABID: Angle Based Intrinsic Dimensionality<br>
 * Proc. 13th Int. Conf. Similarity Search and Applications (SISAP'2020)
 *
 * @author Erik Thordsen
 * @since 0.8.0
 */
@Reference(authors = "Erik Thordsen and Erich Schubert", //
    title = "ABID: Angle Based Intrinsic Dimensionality", //
    booktitle = "Proc. 13th Int. Conf. Similarity Search and Applications (SISAP'2020)", //
    url = "https://doi.org/10.1007/978-3-030-60936-8_17", //
    bibkey = "DBLP:conf/sisap/ThordsenS20")
public class ABIDEstimator extends RABIDEstimator {
  /**
   * Static instance.
   */
  public static final ABIDEstimator STATIC = new ABIDEstimator();

  @Override
  public double estimate(KNNSearcher<DBIDRef> knnq, DistanceQuery<?> distq, DBIDRef cur, int k) {
    return computeABID(distq, knnq.getKNN(cur, k), true /* ABID: false */);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public ABIDEstimator make() {
      return STATIC;
    }
  }
}
