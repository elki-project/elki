package experimentalcode.students.nuecke.optionhandling.parameters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

public class EnumParameter<T extends Enum<T>> extends Parameter<Enum<T>, T> {

  /**
   * Reference to the actual enum type, for T.valueOf().
   */
  protected Class<T> enumClass;

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraint
   *          parameter constraint
   * @param defaultValue
   *          the default value of the parameter
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      List<ParameterConstraint<Enum<T>>> constraint, T defaultValue) {
    super(optionID, constraint, defaultValue);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraints
   *          parameter constraint
   * @param optional
   *          Flag to signal an optional parameter.
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      List<ParameterConstraint<Enum<T>>> constraints, boolean optional) {
    super(optionID, constraints, optional);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraints
   *          parameter constraint
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      List<ParameterConstraint<Enum<T>>> constraints) {
    super(optionID, constraints);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraint
   *          parameter constraint
   * @param defaultValue
   *          the default value of the parameter
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      ParameterConstraint<Enum<T>> constraint, T defaultValue) {
    super(optionID, constraint, defaultValue);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraint
   *          parameter constraint
   * @param optional
   *          Flag to signal an optional parameter.
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      ParameterConstraint<Enum<T>> constraint, boolean optional) {
    super(optionID, constraint, optional);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param constraint
   *          parameter constraint
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID,
      ParameterConstraint<Enum<T>> constraint) {
    super(optionID, constraint);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, and default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param defaultValue
   *          the default value of the parameter
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID, T defaultValue) {
    super(optionID, defaultValue);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param optional
   *          Flag to signal an optional parameter.
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID, boolean optional) {
    super(optionID, optional);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID.
   * 
   * @param optionID
   *          the unique id of the parameter
   */
  public EnumParameter(Class<T> enumClass, OptionID optionID) {
    super(optionID);
    this.enumClass = enumClass;
  }

  @Override
  public String getSyntax() {
    return "<" + joinEnumNames(" | ") + ">";
  }

  @Override
  protected T parseValue(Object obj) throws ParameterException {
    if (obj == null) {
      throw new UnspecifiedParameterException("Parameter \"" + getName()
          + "\": Null value given!");
    }
    if (obj instanceof String) {
      try {
        return Enum.valueOf(enumClass, (String) obj);
      } catch (IllegalArgumentException ex) {
        throw new WrongParameterValueException("Enum parameter " + getName()
            + " is invalid (must be one of [" + joinEnumNames(", ") + "].");
      }
    }
    throw new WrongParameterValueException("Enum parameter " + getName()
        + " is not given as a string.");
  }

  @Override
  public String getValueAsString() {
    return getValue().name();
  }

  /**
   * Get a list of possible values for this enum parameter.
   * 
   * @return list of strings representing possible enum values.
   */
  public Collection<String> getPossibleValues() {
    ArrayList<String> values = new ArrayList<String>();
    for (T t : enumClass.getEnumConstants()) {
      values.add(t.name());
    }
    return values;
  }

  /**
   * Utility method for merging possible values into a string for informational
   * messages.
   * 
   * @param separator char sequence to use as a separator for enum values.
   * @return <code>{VAL1}{separator}{VAL2}{separator}...</code>
   */
  private String joinEnumNames(String separator) {
    try {
      @SuppressWarnings("unchecked")
      T[] enumTypes = (T[]) enumClass.getMethod("values").invoke(enumClass);
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < enumTypes.length; ++i) {
        sb.append(enumTypes[i].name());
        if(i < enumTypes.length - 1) {
          sb.append(separator);
        }
      }
      return sb.toString();
    }
    catch(IllegalArgumentException e) {
      /* eat it */
    }
    catch(SecurityException e) {
      /* eat it */
    }
    catch(IllegalAccessException e) {
      /* eat it */
    }
    catch(InvocationTargetException e) {
      /* eat it */
    }
    catch(NoSuchMethodException e) {
      /* eat it */
    }
    return "enum";
  }

}
