/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.math.statistics;

import elki.math.statistics.kernelfunctions.KernelDensityFunction;

import net.jafama.FastMath;

/**
 * Estimate density given an array of points.
 * <p>
 * Estimates a density using a variable width kernel density estimation.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @assoc - - - KernelDensityFunction
 */
public class KernelDensityEstimator {
  /**
   * Result storage: density
   */
  private double[] dens;

  /**
   * Result storage: variance / quality
   */
  private double[] var;

  /**
   * Initialize and execute kernel density estimation.
   * 
   * @param data data to use (must be sorted!)
   * @param min minimum value
   * @param max maximum value
   * @param kernel Kernel function to use
   * @param window window size
   * @param epsilon Precision threshold
   */
  public KernelDensityEstimator(double[] data, double min, double max, KernelDensityFunction kernel, int window, double epsilon) {
    process(data, min, max, kernel, window, epsilon);
  }

  /**
   * Process an array of data
   * 
   * @param data data to process (must be sorted!)
   * @param kernel Kernel function to use.
   * @param epsilon Precision threshold
   */
  public KernelDensityEstimator(double[] data, KernelDensityFunction kernel, double epsilon) {
    this(data, data[0], data[data.length - 1], kernel,
        // Heuristic for choosing the window size:
        1 + (int) FastMath.log(data.length), epsilon);
  }

  /**
   * Process a new array
   * 
   * @param data data to use (must be sorted!)
   * @param min minimum value
   * @param max maximum value
   * @param kernel Kernel function to use
   * @param window window size
   * @param epsilon Precision threshold
   */
  private void process(double[] data, double min, double max, KernelDensityFunction kernel, int window, double epsilon) {
    dens = new double[data.length];
    var = new double[data.length];

    // This is the desired bandwidth of the kernel.
    double halfwidth = window > 0 ? 0.5 * (max - min) / window : (max - min);
    double ihalfwidth = max > min ? 2 * window / (max - min) : 0.;

    double prev = data[0];
    for(int current = 0; current < data.length; current++) {
      if(prev > (prev = data[current])) {
        throw new IllegalStateException("Input data must be sorted.");
      }
      double value = 0.0;
      for(int i = current; i >= 0; i--) {
        final double contrib = kernel.density(Math.abs(data[i] - data[current]) * ihalfwidth);
        value += contrib;
        if(contrib < epsilon) {
          break;
        }
      }
      for(int i = current + 1; i < data.length; i++) {
        final double contrib = kernel.density(Math.abs(data[i] - data[current]) * ihalfwidth);
        value += contrib;
        if(contrib < epsilon) {
          break;
        }
      }
      double realwidth = Math.min(data[current] + halfwidth, max) - Math.max(min, data[current] - halfwidth);
      dens[current] = value / (data.length * realwidth * .5);
      var[current] = 2 * halfwidth / realwidth;
    }
  }

  /**
   * Retrieve density array (NO copy)
   * 
   * @return density array
   */
  public double[] getDensity() {
    return dens;
  }

  /**
   * Retrieve variance/quality array (NO copy)
   * 
   * @return variance array
   */
  public double[] getVariance() {
    return var;
  }
}
