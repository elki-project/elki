package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Option class specifying a flag object.
 * <p/>
 * A flag object is optional parameter which can be set (value &quot;true&quot;)
 * or not (value &quot;false&quot;).
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class Flag extends Parameter<Boolean, Boolean> {
  /**
   * Constant indicating that the flag is set.
   */
  public static final String SET = "true";

  /**
   * Constant indicating that the flag is not set.
   */
  public static final String NOT_SET = "false";

  /**
   * Constructs a flag object with the given optionID.
   * <p/>
   * If the flag is not set its value is &quot;false&quot;.
   * 
   * @param optionID the unique id of the option
   */
  public Flag(OptionID optionID) {
    super(optionID);
    setOptional(true);
    setDefaultValue(false);
  }

 @Override
  protected Boolean parseValue(Object obj) throws ParameterException {
    if(SET.equals(obj)) {
      return true;
    }
    if(NOT_SET.equals(obj)) {
      return false;
    }
    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }
    if(obj != null && SET.equals(obj.toString())) {
      return true;
    }
    if(obj != null && NOT_SET.equals(obj.toString())) {
      return false;
    }
    throw new WrongParameterValueException("Wrong value for flag \"" + getName() + "\". Allowed values:\n" + SET + " or " + NOT_SET);
  }

  /**
   * A flag has no syntax, since it doesn't take extra options
   */
  @Override
  public String getSyntax() {
    return "<|"+SET+"|"+NOT_SET+">";
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return getValue() ? SET : NOT_SET;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean validate(Boolean obj) throws ParameterException {
    if(obj == null) {
      throw new WrongParameterValueException("Boolean option '" + getName() + "' got 'null' value.");
    }
    return true;
  }

  public void setValue(boolean val) {
    try {
      super.setValue(val);
    }
    catch(ParameterException e) {
      // We're pretty sure that any Boolean is okay.
      LoggingUtil.exception(e);
    }
  }
}