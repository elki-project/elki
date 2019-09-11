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

import java.util.function.Consumer;

import elki.utilities.datastructures.range.IntGenerator;
import elki.utilities.datastructures.range.ParseIntRanges;
import elki.utilities.datastructures.range.StaticIntGenerator;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.ParameterException;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying ranges of integer values.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class IntGeneratorParameter extends AbstractParameter<IntGeneratorParameter, IntGenerator> {
  /**
   * Constructs an integer list parameter
   * 
   * @param optionID the unique id of this parameter
   */
  public IntGeneratorParameter(OptionID optionID) {
    super(optionID);
  }

  @Override
  public String getValueAsString() {
    return getValue().serializeTo(new StringBuilder(1000)).toString();
  }

  @Override
  public String getDefaultValueAsString() {
    return getValue().serializeTo(new StringBuilder(1000)).toString();
  }

  @Override
  protected IntGenerator parseValue(Object obj) throws ParameterException {
    if(obj instanceof int[]) {
      return new StaticIntGenerator((int[]) obj);
    }
    if(obj instanceof IntGenerator) {
      return (IntGenerator) obj;
    }
    if(obj instanceof String && !((String) obj).isEmpty()) {
      try {
        return ParseIntRanges.parseIntRanges((String) obj);
      }
      catch(NumberFormatException e) {
        throw new WrongParameterValueException(this, obj.toString(), "not a valid range of integers", e);
      }
    }
    if(obj instanceof Integer) {
      return new StaticIntGenerator(new int[] { (Integer) obj });
    }
    throw new WrongParameterValueException(this, obj.toString(), "requires a range of integers!");
  }

  /**
   * Returns a string representation of the parameter's type.
   * <p>
   * For a full documentation, see {@link ParseIntRanges}.
   *
   * @return &quot;&lt;start,+=increment,end,...,start,*=factor,end,int1,int2&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<start,+=increment,end,...,start,*=factor,end,int1,int2>";
  }

  /**
   * Get the parameter.
   *
   * @param config Parameterization
   * @param consumer Output consumer
   * @return {@code true} if valid
   */
  public boolean grab(Parameterization config, Consumer<IntGenerator> consumer) {
    if(config.grab(this)) {
      if(consumer != null) {
        consumer.accept(getValue());
      }
      return true;
    }
    return false;
  }
}
