package de.lmu.ifi.dbs.elki.math.statistics.intrinsicdimensionality;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Zipf estimator (qq-estimator) of the intrinsic dimensionality.
 *
 * Unfortunately, this estimator appears to have a bias. We have empirically
 * modified the plot position such that bias is reduced, but could not find the
 * proper way of removing this bias for small samples.
 *
 * References:
 * <p>
 * M. Kratz and S. I. Resnick<br />
 * The QQ-estimator and heavy tails.<br />
 * Stochastic Models, 12(4), 699-724.
 * </p>
 *
 * <p>
 * J. Schultze and J. Steinebach<br />
 * On Least Squares Estimates of an Exponential Tail Coefficient<br />
 * Statistics & Risk Modeling. Band 14, Heft 4
 * </p>
 *
 * <p>
 * J. Beirlant and G. Dierckx and A. Guillou<br />
 * Estimation of the extreme-value index and generalized quantile plots.<br />
 * Bernoulli, 11(6), 949-970.
 * </p>
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "M. Kratz and S. I. Resnick", //
    title = "On Least Squares Estimates of an Exponential Tail Coefficient", //
    booktitle = "Statistics & Risk Modeling. Band 14, Heft 4", //
    url = "http://dx.doi.org/10.1524/strm.1996.14.4.353")
public class ZipfEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final ZipfEstimator STATIC = new ZipfEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = countLeadingZeros(data, adapter, end);
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
      final double logv = Math.log(v);
      final double weight = Math.log(nplus1 / (i - begin + bias));
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ZipfEstimator makeInstance() {
      return STATIC;
    }
  }
}
