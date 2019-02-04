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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions;

/**
 * Quadratic weight function, scaled using the standard deviation.
 * <p>
 * We needed another scaling here, we chose the cutoff point to be 3*stddev. If
 * you need another value, you have to reimplement this class.
 * <p>
 * \( \max\{0.0, 1.0 - \frac{\text{dist}^2}{3\sigma^2} \}\)
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class QuadraticStddevWeight implements WeightFunction {
  /**
   * Scaling: at scaling * stddev the function will hit 0.0
   */
  private static final double scaling = 3;

  /**
   * Evaluate weight function at given parameters. max is ignored.
   */
  @Override
  public double getWeight(double distance, double max, double stddev) {
    if(stddev <= 0) {
      return 1;
    }
    double scaleddistance = distance / (scaling * stddev);
    // After this, the result would be negative.
    if(scaleddistance >= 1.0) {
      return 0.0;
    }
    return 1.0 - scaleddistance * scaleddistance;
  }
}
