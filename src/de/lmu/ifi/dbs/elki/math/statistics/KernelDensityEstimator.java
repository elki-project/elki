package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Estimate density given an array of points.
 * 
 * Estimates a density using a kernel density estimator. Multiple common Kernel
 * functions are supported.
 * 
 * @author Erich Schubert
 * 
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
