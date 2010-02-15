package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;

/**
 * Parameter class for a parameter specifying a pattern.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 */
public class PatternParameter extends Parameter<Pattern, Pattern> {
  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, List<ParameterConstraint<Pattern>> constraint, Pattern defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, List<ParameterConstraint<Pattern>> constraint, String defaultValue) {
    super(optionID, constraint, Pattern.compile(defaultValue));
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraints parameter constraint
   * @param optional Flag to signal an optional parameter.
   */
  public PatternParameter(OptionID optionID, List<ParameterConstraint<Pattern>> constraints, boolean optional) {
    super(optionID, constraints, optional);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraints parameter constraint
   */
  public PatternParameter(OptionID optionID, List<ParameterConstraint<Pattern>> constraints) {
    super(optionID, constraints);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, ParameterConstraint<Pattern> constraint, Pattern defaultValue) {
    super(optionID, constraint, defaultValue);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, ParameterConstraint<Pattern> constraint, String defaultValue) {
    super(optionID, constraint, Pattern.compile(defaultValue));
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   * @param optional Flag to signal an optional parameter.
   */
  public PatternParameter(OptionID optionID, ParameterConstraint<Pattern> constraint, boolean optional) {
    super(optionID, constraint, optional);
  }

  /**
   * Constructs a pattern parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param constraint parameter constraint
   */
  public PatternParameter(OptionID optionID, ParameterConstraint<Pattern> constraint) {
    super(optionID, constraint);
  }

  /**
   * Constructs a pattern parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, Pattern defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructs a pattern parameter with the given optionID, and default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public PatternParameter(OptionID optionID, String defaultValue) {
    super(optionID, Pattern.compile(defaultValue));
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
   * Constructs a pattern parameter with the given optionID.
   * 
   * @param optionID the unique id of the parameter
   */
  public PatternParameter(OptionID optionID) {
    super(optionID);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString() {
    return getValue().toString();
  }

  /** {@inheritDoc} */
  @Override
  protected Pattern parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter \"" + getName() + "\": Null value given!");
    }
    if(obj instanceof Pattern) {
      return (Pattern) obj;
    }
    if(obj instanceof String) {
      try {
        Pattern.compile((String) obj);
      }
      catch(PatternSyntaxException e) {
        throw new WrongParameterValueException("Given pattern \"" + obj + "\" for parameter \"" + getName() + "\" is no valid regular expression!");
      }
    }
    throw new WrongParameterValueException("Given pattern \"" + obj + "\" for parameter \"" + getName() + "\" is of unknown type!");
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
