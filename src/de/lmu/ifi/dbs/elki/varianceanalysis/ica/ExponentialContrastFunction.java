package de.lmu.ifi.dbs.elki.varianceanalysis.ica;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;

/**
 * Provides the function g(x) = x*exp(-x^2/2) which is the derivative of the
 * function G(x)= -exp(-x^2/2).
 * This function is useful, when the independent components
 * are highly super-Gaussian, or when robustness is very
 * important.
 *
 * @author Elke Achtert 
 */
public class ExponentialContrastFunction extends AbstractParameterizable implements ContrastFunction {

  public double function(double x) {
    return (x * Math.exp(-x * x / 2));
  }

  public double derivative(double x) {
    return ((1 - x * x) * Math.exp(-x * x / 2));
  }
}
