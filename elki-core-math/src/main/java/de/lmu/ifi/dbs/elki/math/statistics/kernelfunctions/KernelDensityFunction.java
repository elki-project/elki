package de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Inner function of a kernel density estimator.
 * 
 * Note: as of now, this API does not support asymmetric kernels.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface KernelDensityFunction {
  /**
   * Density contribution of a point at the given relative distance
   * {@code delta >= 0}.
   * 
   * Note that for {@code delta < 0}, in particular for {@code delta < 1}, the
   * results may become invalid. So usually, you will want to invoke this as:
   * 
   * {@code kernel.density(Math.abs(delta))}
   * 
   * @param delta Relative distance
   * @return density contribution
   */
  public double density(double delta);

  /**
   * Get the canonical bandwidth for this kernel.
   * 
   * Note: R uses a different definition of "canonical bandwidth", and also uses
   * differently scaled kernels.
   * 
   * @return Canonical bandwidth
   */
  @Reference(authors = "J.S. Marron, D. Nolan", title = "Canonical kernels for density estimation", booktitle = "Statistics & Probability Letters, Volume 7, Issue 3", url = "http://dx.doi.org/10.1016/0167-7152(88)90050-8")
  public double canonicalBandwidth();

  /**
   * Get the standard deviation of the kernel function.
   * 
   * @return Standard deviation
   */
  public double standardDeviation();

  /**
   * Get the R integral of the kernel, \int K^2(x) dx
   * 
   * TODO: any better name for this?
   * 
   * @return R value
   */
  public double getR();
}
