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
package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Methods of moments estimator, using the first moment (i.e. average).
 * <p>
 * This could be generalized to higher order moments, but the variance increases
 * with the order, and we need this to work well with small sample sizes.
 * <p>
 * Reference:
 * <p>
 * L. Amsaleg, O. Chelly, T. Furon, S. Girard, M. E. Houle, K. Kawarabayashi,
 * M. Nett<br>
 * Estimating Local Intrinsic Dimensionality<br>
 * Proc. SIGKDD Int. Conf. on Knowledge Discovery and Data Mining
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "L. Amsaleg, O. Chelly, T. Furon, S. Girard, M. E. Houle, K. Kawarabayashi, M. Nett", //
    title = "Estimating Local Intrinsic Dimensionality", //
    booktitle = "Proc. SIGKDD International Conference on Knowledge Discovery and Data Mining 2015", //
    url = "https://doi.org/10.1145/2783258.2783405", //
    bibkey = "DBLP:conf/kdd/AmsalegCFGHKN15")
public class MOMEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final MOMEstimator STATIC = new MOMEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int last = end - 1;
    double v1 = 0.;
    int valid = 0;
    for(int i = 0; i < last; i++) {
      final double v = adapter.getDouble(data, i);
      if(v > 0) {
        v1 += v;
        ++valid;
      }
    }
    if(valid <= 1) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    v1 /= valid * adapter.getDouble(data, last);
    return v1 / (1 - v1);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MOMEstimator makeInstance() {
      return STATIC;
    }
  }
}
