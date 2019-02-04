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
package de.lmu.ifi.dbs.elki.math.statistics;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.statistics.kernelfunctions.KernelDensityFunction;
import net.jafama.FastMath;

/**
 * Estimate density given an array of points.
 * 
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
    double halfwidth = ((max - min) / window) * .5;

    for (int current = 0; current < data.length; current++) {
      double value = 0.0;
      for (int i = current; i >= 0; i--) {
        double delta = Math.abs(data[i] - data[current]) / halfwidth;
        final double contrib = kernel.density(delta);
        value += contrib;
        if (contrib < epsilon) {
          break;
        }
      }
      for (int i = current + 1; i < data.length; i++) {
        double delta = Math.abs(data[i] - data[current]) / halfwidth;
        final double contrib = kernel.density(delta);
        value += contrib;
        if (contrib < epsilon) {
          break;
        }
      }
      double realwidth = (Math.min(data[current] + halfwidth, max) - Math.max(min, data[current] - halfwidth));
      double weight = realwidth / (2 * halfwidth);
      dens[current] = value / (data.length * realwidth * .5);
      var[current] = 1 / weight;
    }
  }

  /**
   * Process an array of data
   * 
   * @param data data to process
   * @param kernel Kernel function to use.
   * @param epsilon Precision threshold
   */
  public KernelDensityEstimator(double[] data, KernelDensityFunction kernel, double epsilon) {
    boolean needsort = false;
    for (int i = 1; i < data.length; i++) {
      if (data[i - 1] > data[i]) {
        needsort = true;
        break;
      }
    }
    // Duplicate and sort when needed:
    if (needsort) {
      data = data.clone();
      Arrays.sort(data);
    }
    final double min = data[0];
    final double max = data[data.length - 1];
    // Heuristic for choosing the window size.
    int windows = 1 + (int) (FastMath.log(data.length));

    process(data, min, max, kernel, windows, epsilon);
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
