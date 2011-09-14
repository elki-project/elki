package experimentalcode.students.nuecke.optionhandling.parameters;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Parameter class for a parameter specifying an enum type.
 * 
 * <p>
 * Usage:
 * <pre>
 * // Enum declaration.
 * enum MyEnum { VALUE1, VALUE2 };
 * // Parameter value holder.
 * MyEnum myEnumParameter;
 * 
 * // ...
 * 
 * // Parameterization.
 * EnumParameter&lt;MyEnum&gt; param = new EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class);
 * // OR
 * EnumParameter&lt;MyEnum&gt; param = new EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class, MyEnum.VALUE1);
 * // OR
 * EnumParameter&lt;MyEnum&gt; param = new EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class, true);
 * 
 * if(config.grab(param)) {
 *   myEnumParameter = param.getValue();
 * }
 * </p>
 * 
 * @author Florian Nuecke
 * 
 * @param <E> Enum type
 */
public class EnumParameter<E extends Enum<E>> extends Parameter<Enum<E>, E> {

  /**
   * Reference to the actual enum type, for T.valueOf().
   */
  protected Class<E> enumClass;

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param defaultValue
   *          the default value of the parameter
   */
  public EnumParameter(OptionID optionID, Class<E> enumClass, E defaultValue) {
    super(optionID, defaultValue);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   * @param optional
   *          Flag to signal an optional parameter.
   */
  public EnumParameter(OptionID optionID, Class<E> enumClass, boolean optional) {
    super(optionID, optional);
    this.enumClass = enumClass;
  }

  /**
   * Constructs a enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID
   *          the unique id of the parameter
   */
  public EnumParameter(OptionID optionID, Class<E> enumClass) {
    super(optionID);
    this.enumClass = enumClass;
  }

  @Override
  public String getSyntax() {
    return "<" + joinEnumNames(" | ") + ">";
  }

  @Override
  protected E parseValue(Object obj) throws ParameterException {
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
    for (E t : enumClass.getEnumConstants()) {
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
      E[] enumTypes = (E[]) enumClass.getMethod("values").invoke(enumClass);
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
