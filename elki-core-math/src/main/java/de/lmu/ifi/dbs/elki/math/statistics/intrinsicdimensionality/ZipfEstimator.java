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
import net.jafama.FastMath;

/**
 * Zipf estimator (qq-estimator) of the intrinsic dimensionality.
 * <p>
 * Unfortunately, this estimator appears to have a bias. We have empirically
 * modified the plot position such that bias is reduced, but could not find the
 * proper way of removing this bias for small samples.
 * <p>
 * References:
 * <p>
 * M. Kratz, S. I. Resnick<br>
 * The QQ-estimator and heavy tails<br>
 * Communications in Statistics. Stochastic Models 12(4)
 * <p>
 * J. Schultze, J. Steinebach<br>
 * On Least Squares Estimates of an Exponential Tail Coefficient<br>
 * Statistics and Risk Modeling 14(4)
 * <p>
 * J. Beirlant, G. Dierckx, A. Guillou<br>
 * Estimation of the extreme-value index and generalized quantile plots<br>
 * Bernoulli 11(6)
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "M. Kratz, S. I. Resnick", //
    title = "The QQ-estimator and heavy tails", //
    booktitle = "Communications in Statistics. Stochastic Models 12(4)", //
    url = "https://doi.org/10.1080/15326349608807407", //
    bibkey = "doi:10.1080/15326349608807407")
@Reference(authors = "J. Schultze, J. Steinebach", //
    title = "On Least Squares Estimates of an Exponential Tail Coefficient", //
    booktitle = "Statistics & Risk Modeling 14(4)", //
    url = "https://doi.org/10.1524/strm.1996.14.4.353", //
    bibkey = "doi:10.1524/strm.1996.14.4.353")
@Reference(authors = "J. Beirlant, G. Dierckx, A. Guillou", //
    title = "Estimation of the extreme-value index and generalized quantile plots", //
    booktitle = "Bernoulli 11(6)", //
    url = "https://doi.org/10.3150/bj/1137421635", //
    bibkey = "doi:10.3150/bj/1137421635")
public class ZipfEstimator implements IntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final ZipfEstimator STATIC = new ZipfEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = IntrinsicDimensionalityEstimator.countLeadingZeros(data, adapter, end);
    final int len = end - begin;
    if(len < 2) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    // TODO: any value from literature that works?
    final double bias = .6; // Literature uses 1.
    final double nplus1 = len + bias;
    double wls = 0., ws = 0., ls = 0., wws = 0.;
    for(int i = begin; i < end; ++i) {
      final double v = adapter.getDouble(data, i);
      assert (v > 0.);
      final double logv = FastMath.log(v);
      final double weight = FastMath.log(nplus1 / (i - begin + bias));
      wls += weight * logv;
      ws += weight;
      ls += logv;
      wws += weight * weight;
    }
    return -1. / ((len * wls - ws * ls) / (len * wws - ws * ws));
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ZipfEstimator makeInstance() {
      return STATIC;
    }
  }
}
