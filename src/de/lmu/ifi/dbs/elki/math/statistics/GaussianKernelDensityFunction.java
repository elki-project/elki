package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Gaussian kernel density estimator.
 * 
 * @author Erich Schubert
 */
public final class GaussianKernelDensityFunction implements KernelDensityFunction {
  /**
   * Scaling constant for Gaussian kernel, to make it sum up to 1.
   */
  private static final double GSCALE = 1.0 / Math.sqrt(2.0 * Math.PI);

  @Override
  public double density(double delta) {
    return GSCALE * Math.exp(-.5 * delta * delta);
  }

  /**
   * Private, empty constructor. Use the static instance!
   */
  private GaussianKernelDensityFunction() {
    // Nothing to do.
  }

  /**
   * Static instance.
   */
  public static final GaussianKernelDensityFunction KERNEL = new GaussianKernelDensityFunction();
}
