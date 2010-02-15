package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a string.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class StringParameter extends Parameter<String, String> {
  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public StringParameter(OptionID optionID, List<ParameterConstraint<String>> constraint, String defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraints parameter constraint
   * @param optional Flag to signal an optional parameter.
   */
  public StringParameter(OptionID optionID, List<ParameterConstraint<String>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraints parameter constraint
   */
  public StringParameter(OptionID optionID, List<ParameterConstraint<String>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public StringParameter(OptionID optionID, ParameterConstraint<String> constraint, String defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param optional Flag to signal an optional parameter.
   */
  public StringParameter(OptionID optionID, ParameterConstraint<String> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a string parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   */
  public StringParameter(OptionID optionID, ParameterConstraint<String> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a string parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public StringParameter(OptionID optionID, String defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a string parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   * @param optional Flag to signal an optional parameter.
   */
  public StringParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructs a string parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   */
  public StringParameter(OptionID optionID) {
    super(optionID);
  }
  
  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected String parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter \"" + getName() + "\": Null value given!");
    }
    if (obj instanceof String) {
      return (String) obj;
    }
    // TODO: allow anything convertible by toString()?
    throw new WrongParameterValueException("String parameter "+getName()+" is not a string.");
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;string&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<string>";
  }
}
