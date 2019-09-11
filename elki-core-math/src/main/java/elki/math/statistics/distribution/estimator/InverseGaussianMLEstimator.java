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
package elki.math.statistics.distribution.estimator;

import elki.math.statistics.distribution.InverseGaussianDistribution;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Estimate parameter of the inverse Gaussian (Wald) distribution.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - InverseGaussianDistribution
 */
public class InverseGaussianMLEstimator implements DistributionEstimator<InverseGaussianDistribution> {
  /**
   * Static instance.
   */
  public static final InverseGaussianMLEstimator STATIC = new InverseGaussianMLEstimator();

  @Override
  public <A> InverseGaussianDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double mean = 0.;
    for(int i = 0; i < len; i++) {
      double v = adapter.getDouble(data, i);
      mean += v;
    }
    mean /= len;
    double invmean = 1. / mean;
    double invdev = 0.;
    for(int i = 0; i < len; i++) {
      double v = adapter.getDouble(data, i);
      if(v > 0.) {
        invdev += 1. / v - invmean;
      }
    }
    return new InverseGaussianDistribution(mean, len / invdev);
  }

  @Override
  public Class<? super InverseGaussianDistribution> getDistributionClass() {
    return InverseGaussianDistribution.class;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public InverseGaussianMLEstimator make() {
      return STATIC;
    }
  }
}
