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
package elki.distance.histogram;

import elki.data.NumberVector;
import elki.distance.AbstractNumberVectorDistance;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Distance function based on the Kolmogorov-Smirnov goodness of fit test.
 * <p>
 * This distance function assumes there exist a natural order in the vectors,
 * i.e. they should be some 1-dimensional histogram.
 * <p>
 * The distance is then defined as
 * \[\text{KS}(\vec{x},\vec{y}) := \max_i
 * |\frac{\sum_{j=1}^i x_j}{\sum_{j=1}^d x_j}|
 * - |\frac{\sum_{j=1}^i y_j}{\sum_{j=1}^d y_j}| \]
 * which is the maximum difference of the empirical CDFs,
 * where the divisors normalize the distribution to 1.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class KolmogorovSmirnovDistance extends AbstractNumberVectorDistance {
  /**
   * Static instance. Use this!
   */
  public static final KolmogorovSmirnovDistance STATIC = new KolmogorovSmirnovDistance();

  /**
   * Constructor for the Kolmogorov-Smirnov distance function.
   * 
   * @deprecated Use static instance!
   */
  @Deprecated
  public KolmogorovSmirnovDistance() {
    super();
  }

  @Override
  public double distance(NumberVector v1, NumberVector v2) {
    final int dim = dimensionality(v1, v2);
    double xs = 0., ys = 0., agg = 0.;
    for(int i = 0; i < dim; i++) {
      xs += v1.doubleValue(i);
    }
    for(int i = 0; i < dim; i++) {
      ys += v2.doubleValue(i);
    }
    // Scaling factors:
    double fx = xs > 0 ? 1. / xs : 1, fy = ys > 0 ? 1. / ys : 1;
    xs = ys = 0.; // Reset
    for(int i = 0; i < dim; i++) {
      xs += v1.doubleValue(i);
      ys += v2.doubleValue(i);
      double diff = Math.abs(xs * fx - ys * fy);
      agg = diff < agg ? agg : diff;
    }
    return agg;
  }

  @Override
  public String toString() {
    return "KolmogorovSmirnovDistance";
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class, using the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public KolmogorovSmirnovDistance make() {
      return STATIC;
    }
  }
}
