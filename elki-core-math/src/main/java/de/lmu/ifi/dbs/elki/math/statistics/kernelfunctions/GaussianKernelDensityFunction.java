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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Gaussian kernel density estimator.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public final class GaussianKernelDensityFunction implements KernelDensityFunction {
  /**
   * Static instance.
   */
  public static final GaussianKernelDensityFunction KERNEL = new GaussianKernelDensityFunction();

  /**
   * Canonical bandwidth: (1./(4*pi))^(1/10)
   */
  @Reference(authors = "J. S. Marron, D. Nolan", //
      title = "Canonical kernels for density estimation", //
      booktitle = "Statistics & Probability Letters, Volume 7, Issue 3", //
      url = "https://doi.org/10.1016/0167-7152(88)90050-8", //
      bibkey = "doi:10.1016/0167-71528890050-8")
  public static final double CANONICAL_BANDWIDTH = FastMath.pow(.25 / Math.PI, .1);

  /**
   * R constant.
   */
  public static final double R = .5 * MathUtil.ONE_BY_SQRTPI;

  /**
   * Private, empty constructor. Use the static instance!
   */
  private GaussianKernelDensityFunction() {
    // Nothing to do.
  }

  @Override
  public double density(double delta) {
    return MathUtil.ONE_BY_SQRTTWOPI * FastMath.exp(-.5 * delta * delta);
  }

  @Override
  public double canonicalBandwidth() {
    return CANONICAL_BANDWIDTH;
  }

  @Override
  public double standardDeviation() {
    return 1.;
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
    protected GaussianKernelDensityFunction makeInstance() {
      return KERNEL;
    }
  }
}
