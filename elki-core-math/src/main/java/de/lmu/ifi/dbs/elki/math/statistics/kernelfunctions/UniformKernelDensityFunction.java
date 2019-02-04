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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Uniform / Rectangular kernel density estimator.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public final class UniformKernelDensityFunction implements KernelDensityFunction {
  /**
   * Static instance.
   */
  public static final UniformKernelDensityFunction KERNEL = new UniformKernelDensityFunction();

  /**
   * Canonical bandwidth: (9/2)^(1/5)
   */
  @Reference(authors = "J. S. Marron, D. Nolan", //
      title = "Canonical kernels for density estimation", //
      booktitle = "Statistics & Probability Letters, Volume 7, Issue 3", //
      url = "https://doi.org/10.1016/0167-7152(88)90050-8", //
      bibkey = "doi:10.1016/0167-71528890050-8")
  public static final double CANONICAL_BANDWIDTH = Math.pow(4.5, .2);

  /**
   * Standard deviation.
   */
  private static final double STDDEV = 1. / Math.sqrt(3.);

  /**
   * R constant.
   */
  private static final double R = .5;

  /**
   * Private, empty constructor. Use the static instance!
   */
  private UniformKernelDensityFunction() {
    // Nothing to do.
  }

  @Override
  public double density(double delta) {
    return (delta < 1.) ? .5 : 0.;
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
    protected UniformKernelDensityFunction makeInstance() {
      return KERNEL;
    }
  }
}
