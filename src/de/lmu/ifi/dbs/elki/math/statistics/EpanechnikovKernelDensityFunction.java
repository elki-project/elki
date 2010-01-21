package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Epanechnikov kernel density estimator.
 * 
 * @author Erich Schubert
 */
public final class EpanechnikovKernelDensityFunction implements KernelDensityFunction {
  @Override
  public double density(double delta) {
    if(delta < 1) {
      return 0.75 * (1 - delta * delta);
    }
    return 0;
  }

  /**
   * Private, empty constructor. Use the static instance!
   */
  private EpanechnikovKernelDensityFunction() {
    // Nothing to do.
  }

  /**
   * Static instance.
   */
  public static final EpanechnikovKernelDensityFunction KERNEL = new EpanechnikovKernelDensityFunction();
}
