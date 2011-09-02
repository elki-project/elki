package de.lmu.ifi.dbs.elki.math.statistics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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


/**
 * Estimate density given an array of points.
 * 
 * Estimates a density using a kernel density estimator. Multiple common Kernel
 * functions are supported.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.math.statistics.KernelDensityFunction
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
   * @param data data to use
   * @param min minimum value
   * @param max maximum value
   * @param kernel Kernel function to use
   * @param windows window size
   */
  public KernelDensityEstimator(double[] data, double min, double max, KernelDensityFunction kernel, int windows) {
    process(data, min, max, kernel, windows);
  }
  
  /**
   * Process a new array
   * 
   * @param data data to use
   * @param min minimum value
   * @param max maximum value
   * @param kernel Kernel function to use
   * @param windows window size
   */
  private void process(double[] data, double min, double max, KernelDensityFunction kernel, int windows) {
    dens = new double[data.length];
    var = new double[data.length];

    double halfwidth = ((max - min) / windows) / 2;

    // collect data points
    for(int current = 0; current < data.length; current++) {
      double value = 0.0;
      // TODO: is there any way we can skip through some of the data (at least if its sorted?)
      // Since we know that all kKernels return 0 outside of [-1:1]?
      for(int i = 0; i < data.length; i++) {
        double delta = Math.abs(data[i] - data[current]) / halfwidth;
        value += kernel.density(delta);
      }
      double realwidth = (Math.min(data[current] + halfwidth, max) - Math.max(min, data[current] - halfwidth));
      double weight = realwidth / (2 * halfwidth);
      dens[current] = value / (data.length * realwidth / 2);
      var[current] = 1 / weight;
    }
  }

  /**
   * Process an array of data
   * 
   * @param data data to process
   * @param kernel Kernel function to use.
   */
  public KernelDensityEstimator(double[] data, KernelDensityFunction kernel) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    for(double d : data) {
      if(d < min) {
        min = d;
      }
      if(d > max) {
        max = d;
      }
    }
    // Heuristics.
    int windows = 1 + (int) (Math.log(data.length));

    process(data, min, max, kernel, windows);
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
