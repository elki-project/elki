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
package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Linear Kernel function that computes a similarity between the two feature
 * vectors x and y defined by \(x^T\cdot y\).
 * <p>
 * Note: this is effectively equivalent to using
 * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction}
 * 
 * @author Simon Paradies
 * @since 0.1
 */
@Priority(Priority.RECOMMENDED)
public class LinearKernelFunction extends PolynomialKernelFunction {
  /**
   * Static instance.
   */
  public static final LinearKernelFunction STATIC = new LinearKernelFunction();

  /**
   * Linear kernel. Use static instance {@link #STATIC}!
   */
  @Deprecated
  public LinearKernelFunction() {
    super(1, 0.);
  }

  @Override
  public double similarity(final NumberVector o1, final NumberVector o2) {
    return VectorUtil.dot(o1, o2);
  }

  @Override
  public double distance(final NumberVector fv1, final NumberVector fv2) {
    return FastMath.sqrt(similarity(fv1, fv1) + similarity(fv2, fv2) - 2 * similarity(fv1, fv2));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LinearKernelFunction makeInstance() {
      return LinearKernelFunction.STATIC;
    }
  }
}
