package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter class for a parameter specifying a double value.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class DoubleParameter extends NumberParameter<DoubleParameter, Double> {
  /**
   * Constructs a double parameter with the given optionID and default value.
   * 
   * @param optionID the unique optionID
   * @param defaultValue the default value for this double parameter
   */
  public DoubleParameter(OptionID optionID, double defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a double parameter with the given optionID.
   * 
   * @param optionID the unique id of this parameter
   */
  public DoubleParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  @Override
  protected Double parseValue(Object obj) throws WrongParameterValueException {
    if(obj instanceof Double) {
      return (Double) obj;
    }
    try {
      return ParseUtil.parseDouble(obj.toString());
    }
    catch(NullPointerException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a double value, read: " + obj + "!\n");
    }
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;double&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<double>";
  }

  /**
   * Get the parameter value as double.
   * 
   * @return double value
   */
  public double doubleValue() {
    return getValue().doubleValue();
  }
}
