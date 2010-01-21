package de.lmu.ifi.dbs.elki.math.statistics;

/**
 * Inner function of a kernel density estimator.
 * 
 * @author Erich Schubert
 */
public interface KernelDensityFunction {
  /**
   * Density contribution of a point at the given relative distance {@code delta}.
   * 
   * @param delta Relative distance
   * @return density contribution
   */
  public double density(double delta);
}