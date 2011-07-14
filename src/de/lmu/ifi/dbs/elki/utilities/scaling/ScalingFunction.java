package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for scaling functions used e.g. by outlier evaluation such as
 * Histograms and visualization.
 * 
 * When using outlier scaling functions, make sure to invoke
 * {@link de.lmu.ifi.dbs.elki.utilities.scaling.outlier.OutlierScalingFunction#prepare
 * prepare} to invoke the preprocessing step!
 * 
 * @author Erich Schubert
 */
public interface ScalingFunction extends Parameterizable {
  /**
   * Transform a given value using the scaling function.
   * 
   * @param value Original value
   * @return Scaled value
   */
  public double getScaled(double value);

  /**
   * Get minimum resulting value. May be {@link Double#NaN} or
   * {@link Double#NEGATIVE_INFINITY}.
   * 
   * @return Minimum resulting value.
   */
  public double getMin();

  /**
   * Get maximum resulting value. May be {@link Double#NaN} or
   * {@link Double#POSITIVE_INFINITY}.
   * 
   * @return Maximum resulting value.
   */
  public double getMax();
}