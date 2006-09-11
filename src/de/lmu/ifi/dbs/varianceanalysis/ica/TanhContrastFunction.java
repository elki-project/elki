package de.lmu.ifi.dbs.varianceanalysis.ica;

import java.util.List;

import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides the function g(x) = tanh(a * x) function which is the derivative of the
 * function G(x)= 1/a * log cosh(a * x). This function  is a good general purpose contrast
 * function.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
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
  public static final String A_D = "<double>the parameter a of this function g(x) = tanh(a * x). " +
                                   "Default: " + DEFAULT_A;

  /**
   * Parameter a.
   */
  private double a;

  public TanhContrastFunction() {
    super();
//    optionHandler.put(A_P, new Parameter(A_P, A_D));
    // TODO parameter constraint??
    optionHandler.put(A_P, new DoubleParameter(A_P, A_D));
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
    if (optionHandler.isSet(A_P)) {
      String aString = optionHandler.getOptionValue(A_P);
      try {
        a = Double.parseDouble(aString);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(A_P, aString, A_D, e);
      }
    }
    else {
      a = DEFAULT_A;
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = super.getAttributeSettings();
    AttributeSettings mySettings = settings.get(0);
    mySettings.addSetting(A_P, Double.toString(a));
    return settings;
  }

}
