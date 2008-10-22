package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Estimate density given an array of points.
 * 
 * Estimates a density using a kernel density estimator.
 * Multiple common Kernel functions are supported.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 *
 */
public class KernelDensityEstimator {
  /**
   * Supported kernel functions
   * 
   */
  public static enum Kernel {
    KERNEL_UNIFORM, KERNEL_TRIANGLE, KERNEL_EPANECHNIKOV, KERNEL_GAUSSIAN
  }

  /**
   * Scaling constant for Gaussian kernel
   */
  private static final double GSCALE = 1.0 / Math.sqrt(2.0*Math.PI);
  
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
  public KernelDensityEstimator(double[] data, double min, double max, Kernel kernel, int windows) {
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
  private void process(double[] data, double min, double max, Kernel kernel, int windows) {
    dens = new double[data.length];
    var = new double[data.length];

    double halfwidth = ((max - min) / windows) / 2;

    // collect data points
    for(int current = 0; current < data.length; current++) {
      double value = 0.0;
      for(int i = 0; i < data.length; i++) {
        double delta = Math.abs(data[i] - data[current]) / halfwidth;
        switch(kernel){
        case KERNEL_TRIANGLE:
          if(delta < 1)
            value += 1 - delta;
          break;
        case KERNEL_EPANECHNIKOV:
          if(delta < 1)
            value += 0.75 * (1 - delta * delta);
          break;
        case KERNEL_GAUSSIAN:
          value += GSCALE * Math.exp(-.5 * delta * delta);
          break;
        case KERNEL_UNIFORM:
        default:
          if(delta < 1)
            value += 0.5;
          break;
        }
      }
      double realwidth = (Math.min(data[current] + halfwidth, max) - Math.max(min, data[current] - halfwidth));
      double weight = realwidth / (2 * halfwidth);
      dens[current] = value / (data.length * realwidth / 2); // value / (realwidth * expected);
      var[current] = 1 / weight; // / (weight*weight);

      // System.out.println(xs[current]+" "+ys[current]);
    }
  }

  /**
   * Process an array of data
   * 
   * @param data data to process
   * @param kernel Kernel function to use.
   */
  public KernelDensityEstimator(double[] data, Kernel kernel) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    for(double d : data) {
      if(d < min)
        min = d;
      if(d > max)
        max = d;
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
