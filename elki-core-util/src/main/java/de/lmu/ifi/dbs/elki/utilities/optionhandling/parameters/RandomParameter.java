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
package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Parameter for random generators and/or random seeds.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public class RandomParameter extends AbstractParameter<RandomParameter, RandomFactory> {
  private static final String GLOBAL_RANDOM_STR = "global random";

  /**
   * Seed value, if used
   */
  Long seed = null;

  /**
   * Constructor without default.
   * 
   * @param optionID Option ID
   */
  public RandomParameter(OptionID optionID) {
    super(optionID, RandomFactory.DEFAULT);
  }

  /**
   * Constructor with default value. The default value may be {@code null},
   * which means a new random will be generated.
   * 
   * @param optionID Option ID
   * @param defaultValue Default value. If {@code null}, a new random object
   *        will be created.
   */
  public RandomParameter(OptionID optionID, RandomFactory defaultValue) {
    super(optionID, defaultValue != null ? defaultValue : RandomFactory.DEFAULT);
  }

  /**
   * Constructor with default seed value.
   * 
   * @param optionID Option ID
   * @param seed Default seed.
   */
  public RandomParameter(OptionID optionID, long seed) {
    super(optionID, true);
    this.seed = Long.valueOf(seed);
  }

  @Override
  public String getSyntax() {
    return "<long>";
  }

  @Override
  public void setValue(Object obj) throws ParameterException {
    // This is a bit hackish. Set both seed and random (via super.setValue())
    if(obj == null || obj instanceof RandomFactory) {
      seed = null;
    }
    else if(obj instanceof Long) {
      seed = (Long) obj;
      obj = RandomFactory.get(seed);
    }
    else if(obj instanceof Number) {
      seed = Long.valueOf(((Number) obj).longValue());
      obj = RandomFactory.get(seed);
    }
    else if(GLOBAL_RANDOM_STR.equals(obj) || "global".equals(obj)) {
      obj = RandomFactory.DEFAULT;
    }
    else {
      try {
        seed = Long.valueOf(obj.toString());
        obj = RandomFactory.get(seed);
      }
      catch(NumberFormatException e) {
        throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a long seed value or a random generator factory, read: " + obj + "!\n");
      }
    }
    super.setValue(obj);
  }

  @Override
  protected RandomFactory parseValue(Object obj) throws ParameterException {
    if(obj instanceof RandomFactory) {
      return (RandomFactory) obj;
    }
    if(obj instanceof Long) {
      return RandomFactory.get((Long) obj);
    }
    if(obj instanceof Number) {
      return RandomFactory.get(Long.valueOf(((Number) obj).longValue()));
    }
    try {
      return RandomFactory.get(Long.valueOf(obj.toString()));
    }
    catch(NullPointerException | NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a long seed value or a random generator factory, read: " + obj + "!\n");
    }
  }

  @Override
  public String getValueAsString() {
    return (seed != null) ? seed.toString() //
        : (defaultValue != null) ? defaultValue.toString() //
            : "null";
  }

  @Override
  public String getDefaultValueAsString() {
    if(defaultValue == RandomFactory.DEFAULT) {
      return GLOBAL_RANDOM_STR;
    }
    return super.getDefaultValueAsString();
  }
}
