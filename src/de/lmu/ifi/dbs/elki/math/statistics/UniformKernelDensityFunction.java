package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Uniform / Rectangular kernel density estimator.
 * 
 * @author Erich Schubert
 */
public final class UniformKernelDensityFunction implements KernelDensityFunction {
  @Override
  public double density(double delta) {
    if(delta < 1) {
      return 0.5;
    }
    return 0;
  }

  /**
   * Private, empty constructor. Use the static instance!
   */
  private UniformKernelDensityFunction() {
    // Nothing to do.
  }

  /**
   * Static instance.
   */
  public static final UniformKernelDensityFunction KERNEL = new UniformKernelDensityFunction();
}
