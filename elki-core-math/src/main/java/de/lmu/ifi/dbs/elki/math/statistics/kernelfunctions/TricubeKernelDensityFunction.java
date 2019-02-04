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
package de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Tricube kernel density estimator.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public final class TricubeKernelDensityFunction implements KernelDensityFunction {
  /**
   * Static instance.
   */
  public static final TricubeKernelDensityFunction KERNEL = new TricubeKernelDensityFunction();

  /**
   * Canonical bandwidth.
   * 
   * Computed as (R / STDDEV^4)^(1/5)
   */
  public static final double CANONICAL_BANDWIDTH = Math.pow(59049 / 1729, .2);

  /**
   * Standard deviation.
   */
  private static final double STDDEV = Math.sqrt(35. / 243.);

  /**
   * R constant.
   */
  private static final double R = 175. / 247.;

  /**
   * Scaling factor.
   */
  private final double SCALE = 70. / 81.;

  /**
   * Private, empty constructor. Use the static instance!
   */
  private TricubeKernelDensityFunction() {
    // Nothing to do.
  }

  @Override
  public double density(double delta) {
    if (delta >= 1.) {
      return 0;
    }
    final double u = 1 - delta * delta * delta;
    return SCALE * u * u * u;
  }

  @Override
  public double canonicalBandwidth() {
    return CANONICAL_BANDWIDTH;
  }

  @Override
  public double standardDeviation() {
    return STDDEV;
  }

  @Override
  public double getR() {
    return R;
  }

  /**
   * Parameterization stub.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected TricubeKernelDensityFunction makeInstance() {
      return KERNEL;
    }
  }
}
