package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying an integer value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class IntParameter extends NumberParameter<IntParameter, Integer> {
  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   * @param defaultValue the default value
   */
  public IntParameter(OptionID optionID, int defaultValue) {
    super(optionID, Integer.valueOf(defaultValue));
  }

  /**
   * Constructs an integer parameter with the given optionID.
   * 
   * @param optionID optionID the unique id of the option
   */
  public IntParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Integer parseValue(Object obj) throws ParameterException {
    if(obj instanceof Integer) {
      return (Integer) obj;
    }
    try {
      final String s = obj.toString();
      return (int) ParseUtil.parseLongBase10(s);
    }
    catch(NullPointerException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires an integer value, read: " + obj + "!\n");
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires an integer value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;int&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<int>";
  }

  /**
   * Get the parameter value as integer
   * 
   * @return Parameter value
   */
  public int intValue() {
    return getValue().intValue();
  }
}
