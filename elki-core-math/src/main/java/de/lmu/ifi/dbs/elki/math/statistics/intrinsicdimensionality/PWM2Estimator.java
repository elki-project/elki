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
 * Probability weighted moments based estimator, using the second moment.
 * <p>
 * It can be shown theoretically that this estimator is expected to have a
 * higher variance than the one using the first moment only, it is included for
 * completeness only.
 * <p>
 * Reference:
 * <p>
 * L. Amsaleg, O. Chelly, T. Furon, S. Girard, M. E. Houle, K. Kawarabayashi,
 * M. Nett<br>
 * Estimating Local Intrinsic Dimensionality<br>
 * Proc. SIGKDD Int. Conf. on Knowledge Discovery and Data Mining
 * <p>
 * We use the unbiased weights of Maciunas Landwehr et al.:
 * <p>
 * J. Maciunas Landwehr, N. C. Matalas, J. R. Wallis<br>
 * Probability weighted moments compared with some traditional techniques in
 * estimating Gumbel parameters and quantiles<br>
 * Water Resources Research 15(5)
 * <p>
 * but we pretend we had two additional data points at 0, to not lose valuable
 * data. When implemented exactly, we would have to assign a weight of 0 to the
 * first point. But since we are not using the lower PWMs, we don't want to do
 * this. This hack causes this estimator to have a bias to underestimate the ID.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "L. Amsaleg, O. Chelly, T. Furon, S. Girard, M. E. Houle, K. Kawarabayashi, M. Nett", //
    title = "Estimating Local Intrinsic Dimensionality", //
    booktitle = "Proc. SIGKDD International Conference on Knowledge Discovery and Data Mining 2015", //
    url = "https://doi.org/10.1145/2783258.2783405", //
    bibkey = "DBLP:conf/kdd/AmsalegCFGHKN15")
@Reference(authors = "J. Maciunas Landwehr, N. C. Matalas, J. R. Wallis", //
    title = "Probability weighted moments compared with some traditional techniques in estimating Gumbel parameters and quantiles", //
    booktitle = "Water Resources Research 15(5)", //
    url = "https://doi.org/10.1029/WR015i005p01055", //
    bibkey = "doi:10.1029/WR015i005p01055")
public class PWM2Estimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final PWM2Estimator STATIC = new PWM2Estimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = IntrinsicDimensionalityEstimator.countLeadingZeros(data, adapter, end);
    if(end - begin <= 3) {
      if(end - begin == 2) { // Fallback to MoM
        double v1 = adapter.getDouble(data, begin) / adapter.getDouble(data, begin + 1);
        return v1 / (1 - v1);
      }
      if(end - begin == 3) { // Fallback to first moment only
        double v1 = adapter.getDouble(data, begin + 1) * .5 / adapter.getDouble(data, begin + 2);
        return v1 / (1 - 2 * v1);
      }
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    final int last = end - 1; // Except for last
    // Estimate second PWM (k=2)
    // In the following, we pretend we had TWO more data points!
    double v2 = 0.;
    int valid = 0;
    for(int j = begin; j < last; j++, valid++) {
      v2 += adapter.getDouble(data, j) * (valid + 2) * (valid + 1);
    }
    // All scaling factors collected for performance reasons
    final double w = adapter.getDouble(data, last);
    v2 /= (valid + 2) * w * (valid + 1) * valid;
    return v2 / (1 - 3 * v2);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected PWM2Estimator makeInstance() {
      return STATIC;
    }
  }
}
