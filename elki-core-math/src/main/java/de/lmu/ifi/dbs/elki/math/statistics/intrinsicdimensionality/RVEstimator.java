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
 * Regularly Varying Functions estimator of the intrinsic dimensionality
 *
 * Reference:
 * <p>
 * L. Amsaleg and O. Chelly and T. Furon and S. Girard and M. E. Houle and K.
 * Kawarabayashi and M. Nett<br />
 * Estimating Local Intrinsic Dimensionality<br />
 * Proc. SIGKDD International Conference on Knowledge Discovery and Data Mining
 * 2015
 * </p>
 *
 * @author Oussama Chelly
 * @since 0.7.0
 */
@Reference(authors = "L. Amsaleg and O. Chelly and T. Furon and S. Girard and M. E. Houle and K. Kawarabayashi and M. Nett", //
    title = "Estimating Local Intrinsic Dimensionality", //
    booktitle = "Proc. SIGKDD International Conference on Knowledge Discovery and Data Mining 2015", //
    url = "http://dx.doi.org/10.1145/2783258.2783405")
public class RVEstimator extends AbstractIntrinsicDimensionalityEstimator {
  /**
   * Static instance.
   */
  public static final RVEstimator STATIC = new RVEstimator();

  @Override
  public <A> double estimate(A data, NumberArrayAdapter<?, ? super A> adapter, final int end) {
    final int begin = countLeadingZeros(data, adapter, end);
    final int k = end - begin;
    if(k < 2) {
      throw new ArithmeticException("ID estimates require at least 2 non-zero distances");
    }
    final int n1 = k >> 1; // i in Section 5.1 of the ACM publication
    final int n2 = (3 * k) >> 2; // j in Section 5.1 of the ACM publication
    final int n3 = k; // n in Section 5.1 of the ACM publication
    final double r1 = adapter.getDouble(data, begin + n1 - 1);
    final double r2 = adapter.getDouble(data, begin + n2 - 1);
    final double r3 = adapter.getDouble(data, begin + n3 - 1);
    final double p = (r3 - r2) / (r1 - 2 * r2 + r3);
    // Optimized away: final double a1 = 1;
    final double a2 = (1. - p) / p;
    return (/* a1 * */ Math.log(n3 / (double) n2) //
        + a2 * Math.log(n1 / (double) n2)) / (/* a1 * */ Math.log(r3 / r2) + a2 * Math.log(r1 / r2));
  }

  /**
   * Parameterization class.
   *
   * @author Oussama Chelly
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RVEstimator makeInstance() {
      return STATIC;
    }
  }
}
