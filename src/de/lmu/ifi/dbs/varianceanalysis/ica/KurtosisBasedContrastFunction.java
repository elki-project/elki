package de.lmu.ifi.dbs.varianceanalysis.ica;

import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;

/**
 * Provides the function g(x) = x^3 which is the derivative of the fourth power as in kurtosis.
 * This function is useful for estimating sub-Gaussian
 * independent components when there are no outliers.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class KurtosisBasedContrastFunction extends AbstractParameterizable implements ContrastFunction {
  /**
   * @see ContrastFunction#function(double)
   */
  public double function(double x) {
    return (x * x * x);
  }

  /**
   * @see ContrastFunction#derivative(double)
   */
  public double derivative(double x) {
    return (3 * x * x);
  }
}
