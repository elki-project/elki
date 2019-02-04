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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.RayleighDistribution;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Estimate the scale parameter of a (non-shifted) RayleighDistribution using
 * the method of L-Moments (LMM).
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @navassoc - estimates - RayleighDistribution
 */
public class RayleighLMMEstimator implements LMMDistributionEstimator<RayleighDistribution> {
  /**
   * Static instance.
   */
  public static final RayleighLMMEstimator STATIC = new RayleighLMMEstimator();

  /**
   * Constructor. Private: use static instance!
   */
  private RayleighLMMEstimator() {
    super();
  }

  @Override
  public int getNumMoments() {
    return 2;
  }

  @Override
  public RayleighDistribution estimateFromLMoments(double[] xmom) {
    double sigma = 2. * xmom[1] / (MathUtil.SQRTPI * (MathUtil.SQRT2 - 1.));
    double mu = xmom[0] - sigma * MathUtil.SQRTHALFPI;
    return new RayleighDistribution(mu, sigma);
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
    protected RayleighLMMEstimator makeInstance() {
      return STATIC;
    }
  }
}
