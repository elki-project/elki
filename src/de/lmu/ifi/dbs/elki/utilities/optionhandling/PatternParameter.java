package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.ArrayList;

/**
 * Parameter class for a parameter specifying a pattern.
 * 
 * @author Steffi Wanka
 */
public class PatternParameter extends Parameter<String, String> {
  /**
   * Constructs a pattern parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   */
  public PatternParameter(OptionID optionID) {
    super(optionID);
  }

  /**
   * Constructs a pattern parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   * @param optional Flag to signal an optional parameter.
   */
  public PatternParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a pattern parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, String defaultValue) {
    super(optionID, new ArrayList<ParameterConstraint<String>>(), false, defaultValue);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, ParameterConstraint<String> constraint, String defaultValue) {
    this(optionID, defaultValue);
    addConstraint(constraint);
  }

  @Override
  public void setValue(String value) throws ParameterException {
    super.setValue(value);
    if(isValid(value)) {
      this.value = value;
    }
  }

  @Override
  public boolean isValid(String value) throws ParameterException {
    try {
      Pattern.compile(value);
    }
    catch(PatternSyntaxException e) {
      throw new WrongParameterValueException("Given pattern \"" + value + "\" for parameter \"" + getName() + "\" is no valid regular expression!");
    }
    try {
      for(ParameterConstraint<String> con : this.constraints) {
        con.test(value);
      }
    }
    catch(ParameterException ex) {
      throw new WrongParameterValueException("Parameter constraint error for pattern" + " parameter " + getName() + ": " + ex.getMessage());
    }
    return true;
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;pattern&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<pattern>";
  }
}
