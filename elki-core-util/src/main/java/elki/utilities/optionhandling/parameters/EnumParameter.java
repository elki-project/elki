/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.utilities.optionhandling.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.UnspecifiedParameterException;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying an enum type.
 * <p>
 * Usage:
 * 
 * <pre>
 * // Enum declaration.
 * enum MyEnum { VALUE1, VALUE2 };
 * // Parameter value holder.
 * MyEnum myEnumParameter;
 * 
 * // ...
 * 
 * // Parameterization.
 * EnumParameter&lt;MyEnum&gt; param = new
 * EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class);
 * // OR
 * EnumParameter&lt;MyEnum&gt; param = new
 * EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class, MyEnum.VALUE1);
 * // OR
 * EnumParameter&lt;MyEnum&gt; param = new
 * EnumParameter&lt;MyEnum&gt;(ENUM_PROPERTY_ID, MyEnum.class, true);
 * 
 * if(config.grab(param)) {
 *   myEnumParameter = param.getValue();
 * }
 * </pre>
 * 
 * @author Florian Nuecke
 * @since 0.4.0
 * 
 * @param <E> Enum type
 */
public class EnumParameter<E extends Enum<E>> extends AbstractParameter<EnumParameter<E>, E> {
  /**
   * Reference to the actual enum type, for T.valueOf().
   */
  protected Class<E> enumClass;

  /**
   * Constructs an enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
   * @param defaultValue the default value of the parameter
   */
  public EnumParameter(OptionID optionID, Class<E> enumClass, E defaultValue) {
    super(optionID, defaultValue);
    this.enumClass = enumClass;
  }

  /**
   * Constructs an enum parameter with the given optionID, constraints and
   * default value.
   * 
   * @param optionID the unique id of the parameter
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
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(enumClass.isInstance(obj)) {
      return enumClass.cast(obj);
    }
    if(obj instanceof String) {
      try {
        return Enum.valueOf(enumClass, (String) obj);
      }
      catch(IllegalArgumentException ex) {
        throw new WrongParameterValueException("Enum parameter " + getOptionID().getName() + " is invalid (must be one of [" + joinEnumNames(", ") + "].");
      }
    }
    throw new WrongParameterValueException("Enum parameter " + getOptionID().getName() + " is not given as a string.");
  }

  @Override
  public String getValueAsString() {
    return getValue().name();
  }

  @Override
  public StringBuilder describeValues(StringBuilder buf) {
    buf.append("One of:").append(FormatUtil.NEWLINE);
    for(String s : getPossibleValues()) {
      buf.append("->").append(FormatUtil.NONBREAKING_SPACE).append(s).append(FormatUtil.NEWLINE);
    }
    return buf;
  }

  /**
   * Get a list of possible values for this enum parameter.
   * 
   * @return list of strings representing possible enum values.
   */
  public Collection<String> getPossibleValues() {
    // Convert to string array
    final E[] enums = enumClass.getEnumConstants();
    ArrayList<String> values = new ArrayList<>(enums.length);
    for(E t : enums) {
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
    E[] enumTypes = enumClass.getEnumConstants();
    StringBuilder sb = new StringBuilder(enumTypes.length * 20);
    for(int i = 0; i < enumTypes.length; ++i) {
      sb.append(enumTypes[i].name()).append(separator);
    }
    sb.setLength(Math.max(0, sb.length() - separator.length()));
    return sb.toString();
  }

  /**
   * Get the parameter.
   *
   * @param config Parameterization
   * @param consumer Output consumer
   * @return {@code true} if valid
   */
  public boolean grab(Parameterization config, Consumer<E> consumer) {
    if(config.grab(this)) {
      if(consumer != null) {
        consumer.accept(getValue());
      }
      return true;
    }
    return false;
  }
}
