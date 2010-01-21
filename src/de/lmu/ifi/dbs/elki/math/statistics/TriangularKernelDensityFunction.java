package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Triangular kernel density estimator.
 * 
 * @author Erich Schubert
 */
public final class TriangularKernelDensityFunction implements KernelDensityFunction {
  @Override
  public double density(double delta) {
    if(delta < 1) {
      return 1 - delta;
    }
    return 0;
  }

  /**
   * Private, empty constructor. Use the static instance!
   */
  private TriangularKernelDensityFunction() {
    // Nothing to do.
  }

  /**
   * Static instance.
   */
  public static final TriangularKernelDensityFunction KERNEL = new TriangularKernelDensityFunction();
}
