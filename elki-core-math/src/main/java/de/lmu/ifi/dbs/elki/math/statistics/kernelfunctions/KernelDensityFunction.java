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

/**
 * Inner function of a kernel density estimator.
 * <p>
 * Note: as of now, this API does not support asymmetric kernels, which would be
 * difficult in the multivariate case.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface KernelDensityFunction {
  /**
   * Density contribution of a point at the given relative distance
   * {@code delta >= 0}.
   * <p>
   * Note that for {@code delta < 0}, in particular for {@code delta < 1}, the
   * results may become invalid. So usually, you will want to invoke this as:
   * <p>
   * {@code kernel.density(Math.abs(delta))}
   * 
   * @param delta Relative distance
   * @return density contribution
   */
  double density(double delta);

  /**
   * Get the canonical bandwidth for this kernel.
   * <p>
   * Note: R uses a different definition of "canonical bandwidth", and also uses
   * differently scaled kernels.
   * 
   * @return Canonical bandwidth
   */
  @Reference(authors = "J. S. Marron, D. Nolan", //
      title = "Canonical kernels for density estimation", //
      booktitle = "Statistics & Probability Letters, Volume 7, Issue 3", //
      url = "https://doi.org/10.1016/0167-7152(88)90050-8", //
      bibkey = "doi:10.1016/0167-71528890050-8")
  double canonicalBandwidth();

  /**
   * Get the standard deviation of the kernel function.
   * 
   * @return Standard deviation
   */
  double standardDeviation();

  /**
   * Get the R integral of the kernel, \int K^2(x) dx
   * <p>
   * TODO: any better name for this?
   * 
   * @return R value
   */
  double getR();
}
