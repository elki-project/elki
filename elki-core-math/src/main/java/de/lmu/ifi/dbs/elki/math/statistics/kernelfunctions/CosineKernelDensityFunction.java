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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Cosine kernel density estimator.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public final class CosineKernelDensityFunction implements KernelDensityFunction {
  /**
   * Static instance.
   */
  public static final CosineKernelDensityFunction KERNEL = new CosineKernelDensityFunction();

  /**
   * Canonical bandwidth.
   * 
   * Computed as (R / STDDEV^4)^(1/5)
   * 
   * This is approximately: 1.7662654022050532
   */
  public static final double CANONICAL_BANDWIDTH = Math.pow(MathUtil.PISQUARE / (16. * (1. - 8. / MathUtil.PISQUARE) * (1. - 8. / MathUtil.PISQUARE)), .2);

  /**
   * Standard deviation.
   */
  private static final double STDDEV = Math.sqrt(1. - 8. / MathUtil.PISQUARE);

  /**
   * R constant.
   */
  private static final double R = Math.PI * Math.PI / 16.;

  /**
   * Private, empty constructor. Use the static instance!
   */
  private CosineKernelDensityFunction() {
    // Nothing to do.
  }

  @Override
  public double density(double delta) {
    return (delta < 1.) ? MathUtil.QUARTERPI * FastMath.cos(MathUtil.HALFPI * delta) : 0.;
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
    protected CosineKernelDensityFunction makeInstance() {
      return KERNEL;
    }
  }
}
