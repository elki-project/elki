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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Slightly improved estimation, that takes sample size into account and
 * enhances the interval appropriately.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - UniformDistribution
 */
public class UniformEnhancedMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
  /**
   * Slightly more refined estimator: takes sample size into account.
   */
  public static final UniformEnhancedMinMaxEstimator STATIC = new UniformEnhancedMinMaxEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private UniformEnhancedMinMaxEstimator() {
    super();
  }

  @Override
  public <A> UniformDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    DoubleMinMax mm = new DoubleMinMax();
    for (int i = 0; i < len; i++) {
      final double val = adapter.getDouble(data, i);
      if (val > Double.NEGATIVE_INFINITY && val < Double.POSITIVE_INFINITY) {
        mm.put(val);
      }
    }
    return estimate(mm.getMin(), mm.getMax(), len);
  }

  /**
   * Estimate from simple characteristics.
   * 
   * @param min Minimum
   * @param max Maximum
   * @param count Number of observations
   * @return Distribution
   */
  public UniformDistribution estimate(double min, double max, final int count) {
    double grow = (count > 1) ? 0.5 * (max - min) / (count - 1) : 0.;
    return new UniformDistribution(Math.max(min - grow, -Double.MAX_VALUE), Math.min(max + grow, Double.MAX_VALUE));
  }

  @Override
  public Class<? super UniformDistribution> getDistributionClass() {
    return UniformDistribution.class;
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected UniformEnhancedMinMaxEstimator makeInstance() {
      return STATIC;
    }
  }
}
