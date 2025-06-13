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

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import net.jafama.FastMath;

/**
 * Hill estimator of the intrinsic dimensionality (maximum likelihood estimator
 * for ID).
 * <p>
 * Reference:
 * <p>
 * B. M. Hill<br>
 * A simple general approach to inference about the tail of a distribution<br>
 * The annals of statistics 3(5)
 * <p>
 * E. Levina, P. J. Bickel<br>
 * Maximum Likelihood Estimation of Intrinsic Dimension<br>
 * Neural Information Processing Systems (NIPS) 2004
 *
 * @author Jonathan von Brünken
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "B. M. Hill", //
    title = "A simple general approach to inference about the tail of a distribution", //
    booktitle = "The annals of statistics 3(5)", //
    url = "https://doi.org/10.1214/aos/1176343247", //
    bibkey = "doi:10.1214/aos/1176343247")
@Reference(authors = "E. Levina, P. J. Bickel", //
    title = "Maximum Likelihood Estimation of Intrinsic Dimension", //
    booktitle = "Neural Information Processing Systems (NIPS) 2004", //
    url = "https://proceedings.neurips.cc/paper/2004/hash/74934548253bcab8490ebd74afed7031-Abstract.html", //
    bibkey = "DBLP:conf/nips/LevinaB04")
public class HillEstimator implements DistanceBasedIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final HillEstimator STATIC = new HillEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int last = end - 1;
    final double w = adapter.getDouble(data, last);
    if(w <= 0.) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    final double halfw = 0.5 * w;
    double sum = 0.;
    int valid = 0;
    for(int i = 0; i < last; ++i) {
      final double v = adapter.getDouble(data, i);
      if(!(v > 0.)) {
        continue;
      }
      sum += v < halfw ? Math.log(v / w) : FastMath.log1p((v - w) / w);
      ++valid;
    }
    if(valid < 1) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    return -valid / sum;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public HillEstimator make() {
      return STATIC;
    }
  }
}
