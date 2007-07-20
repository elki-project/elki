package de.lmu.ifi.dbs.varianceanalysis.ica;

import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Provides the function g(x) = tanh(a * x) function which is the derivative of the
 * function G(x)= 1/a * log cosh(a * x). This function  is a good general purpose contrast
 * function.
 *
 * @author Elke Achtert 
 */
public class TanhContrastFunction extends AbstractParameterizable implements ContrastFunction {
  /**
   * Parameter for a.
   */
  public static final String A_P = "a";

  /**
   * The default a.
   */
  public static final double DEFAULT_A = 1.0;

  /**
   * Description for parameter a.
   */
  public static final String A_D = "the parameter a of this function g(x) = tanh(a * x). " +
                                   "Default: " + DEFAULT_A;

  /**
   * Parameter a.
   */
  private double a;

  public TanhContrastFunction() {
    super();

    DoubleParameter a = new DoubleParameter(A_P, A_D);
    a.setDefaultValue(DEFAULT_A);
    optionHandler.put(A_P, a);
  }

  /**
   * @see ContrastFunction#function(double)
   */
  public double function(double x) {
    return (Math.tanh(a * x));
  }

  /**
   * @see ContrastFunction#derivative(double)
   */
  public double derivative(double x) {
    double tanha1x = Math.tanh(a * x);
    return (a * (1 - tanha1x * tanha1x));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // a
    a = (Double)optionHandler.getOptionValue(A_P);

    return remainingParameters;
  }
}
