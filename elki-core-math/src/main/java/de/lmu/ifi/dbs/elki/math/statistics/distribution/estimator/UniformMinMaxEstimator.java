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
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the uniform distribution by computing min and max.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - UniformDistribution
 */
public class UniformMinMaxEstimator implements DistributionEstimator<UniformDistribution> {
  /**
   * The most naive estimator possible: uses minimum and maximum.
   */
  public static final UniformMinMaxEstimator STATIC = new UniformMinMaxEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private UniformMinMaxEstimator() {
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
    return estimate(mm);
  }

  /**
   * Estimate parameters from minimum and maximum observed.
   * 
   * @param mm Minimum and Maximum
   * @return Estimation
   */
  public UniformDistribution estimate(DoubleMinMax mm) {
    return new UniformDistribution(Math.max(mm.getMin(), -Double.MAX_VALUE), Math.min(mm.getMax(), Double.MAX_VALUE));
  }

  /**
   * Estimate parameters from minimum and maximum observed.
   * 
   * @param min Minimum
   * @param max Maximum
   * @return Estimation
   */
  public Distribution estimate(double min, double max) {
    return new UniformDistribution(min, max);
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
    protected UniformMinMaxEstimator makeInstance() {
      return STATIC;
    }
  }
}
