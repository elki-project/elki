package de.lmu.ifi.dbs.elki.utilities.scaling;

/**
 * Interface for scaling functions used by Outlier evaluation such as Histograms
 * and visualization.
 * 
 * @author Erich Schubert
 */
public interface ScalingFunction {
  /**
   * Transform a given value using the scaling function.
   * 
   * @param value Original value
   * @return Scaled value
   */
  public double getScaled(double value);
  
  /**
   * Get minimum resulting value. May be {@link Double#NaN} or {@link Double#NEGATIVE_INFINITY}.
   * @return Minimum resulting value.
   */
  public double getMin();
  
  /**
   * Get maximum resulting value. May be {@link Double#NaN} or {@link Double#POSITIVE_INFINITY}.
   * @return Maximum resulting value.
   */
  public double getMax();
}
