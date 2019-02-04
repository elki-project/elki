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

import de.lmu.ifi.dbs.elki.math.statistics.distribution.RayleighDistribution;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the scale parameter of a (non-shifted) RayleighDistribution using a
 * maximum likelihood estimate.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - RayleighDistribution
 */
public class RayleighMLEEstimator implements DistributionEstimator<RayleighDistribution> {
  /**
   * Static instance.
   */
  public static final RayleighMLEEstimator STATIC = new RayleighMLEEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private RayleighMLEEstimator() {
    super();
  }

  @Override
  public <A> RayleighDistribution estimate(A data, NumberArrayAdapter<?, A> adapter) {
    final int len = adapter.size(data);
    double sumsq = 0.;
    for(int i = 0; i < len; i++) {
      double v = adapter.getDouble(data, i);
      sumsq += v * v;
    }
    return new RayleighDistribution(Math.sqrt(.5 * sumsq / len));
  }

  @Override
  public Class<? super RayleighDistribution> getDistributionClass() {
    return RayleighDistribution.class;
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
    protected RayleighMLEEstimator makeInstance() {
      return STATIC;
    }
  }
}
