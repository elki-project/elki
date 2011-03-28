package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Length constraint for a
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter}
 * .
 * 
 * @author Erich Schubert
 */
public class StringLengthConstraint implements ParameterConstraint<String> {
  /**
   * Minimum length
   */
  int minlength;

  /**
   * Maximum length
   */
  int maxlength;

  /**
   * Constructor with minimum and maximum length.
   * 
   * @param minlength Minimum length, may be 0 for no limit
   * @param maxlength Maximum length, may be -1 for no limit
   */
  public StringLengthConstraint(int minlength, int maxlength) {
    super();
    this.minlength = minlength;
    this.maxlength = maxlength;
  }

  /**
   * Checks if the given string value of the string parameter is within the
   * length restrictions. If not, a parameter exception is thrown.
   */
  @Override
  public void test(String t) throws ParameterException {
    if(t.length() < minlength) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value length must be at least " + minlength + ".");
    }
    if(maxlength > 0 && t.length() > maxlength) {
      throw new WrongParameterValueException("Parameter Constraint Error.\n" + "Parameter value length must be at most " + maxlength + ".");
    }
  }

  @Override
  public String getDescription(String parameterName) {
    if(maxlength > 0) {
      return parameterName + " has length " + minlength + " to " + maxlength + ".";
    }
    else {
      return parameterName + " has length of at least " + minlength + ".";
    }
  }
}
