package de.lmu.ifi.dbs.elki.varianceanalysis.ica;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Provides the function g(x) = tanh(a * x) function which is the derivative of the
 * function G(x)= 1/a * log cosh(a * x). This function  is a good general purpose contrast
 * function.
 *
 * @author Elke Achtert 
 */
public class TanhContrastFunction extends AbstractParameterizable implements ContrastFunction {
  /**
   * The default a.
   */
  public static final double DEFAULT_A = 1.0;

  /**
   * OptionID for {@link #A_PARAM}
   */
  public static final OptionID A_ID = OptionID.getOrCreateOptionID("tanh.a",
      "the parameter a of this function g(x) = tanh(a * x). " +
      "Default: " + DEFAULT_A);
  
  /**
   * Parameter for a
   */
  private final DoubleParameter A_PARAM = new DoubleParameter(A_ID, DEFAULT_A);

  /**
   * Parameter a.
   */
  private double a;

  public TanhContrastFunction() {
    super();

    addOption(A_PARAM);
  }

  public double function(double x) {
    return (Math.tanh(a * x));
  }

  public double derivative(double x) {
    double tanha1x = Math.tanh(a * x);
    return (a * (1 - tanha1x * tanha1x));
  }

  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // a
    a = A_PARAM.getValue();

    return remainingParameters;
  }
}
