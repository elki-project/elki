package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface for scaling functions used by Outlier evaluation such as Histograms
 * and visualization.
 * 
 * When the function needs extra parameters, it should also implement {@link Parameterizable}.
 * 
 * @author Erich Schubert
 * 
 */
public interface ScalingFunction {
  /**
   * Transform a given value using the scaling function.
   * 
   * @param value Original value
   * @return Scaled value
   */
  public double getScaled(double value);
}
