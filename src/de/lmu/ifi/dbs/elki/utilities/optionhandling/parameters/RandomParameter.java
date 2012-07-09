package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Random;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Parameter for random generators and/or random seeds.
 * 
 * @author Erich Schubert
 */
public class RandomParameter extends AbstractParameter<Random, Random> {
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
    super(optionID);
  }

  /**
   * Constructor with optional flag.
   * 
   * Note: you probably mean to use {@link #RandomParameter(OptionID, null)},
   * which will use a random seed.
   * 
   * @param optionID Option ID
   * @param optional Flag to indicate the value is optional
   */
  public RandomParameter(OptionID optionID, boolean optional) {
    super(optionID, optional);
  }

  /**
   * Constructor with default value. The default value may be {@code null},
   * which means a new random will be generated.
   * 
   * @param optionID Option ID
   * @param defaultValue Default value. If {@code null}, a new random object
   *        will be created.
   */
  public RandomParameter(OptionID optionID, Random defaultValue) {
    super(optionID, defaultValue);
  }

  /**
   * Constructor with default value. The default value may be {@code null},
   * which means a new random will be generated.
   * 
   * @param optionID Option ID
   * @param defaultValue Default value. If {@code null}, a new random object
   *        will be created.
   */
  public RandomParameter(OptionID optionID, long seed) {
    super(optionID);
    this.seed = seed;
  }

  @Override
  public String getSyntax() {
    return "<long|Random>";
  }

  @Override
  public void setValue(Object obj) throws ParameterException {
    // This is a bit hackish. Set both seed and random (via super.setValue())
    if(obj instanceof Random) {
      seed = null;
    }
    else if(obj instanceof Long) {
      seed = (Long) obj;
      obj = new Random(seed);
    }
    else if(obj instanceof Integer) {
      seed = (long) (Integer) obj;
      obj = new Random(seed);
    }
    else {
      try {
        seed = Long.parseLong(obj.toString());
        obj = new Random(seed);
      }
      catch(NullPointerException e) {
        throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long seed value or a random generator, read: " + obj + "!\n");
      }
      catch(NumberFormatException e) {
        throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long seed value or a random generator, read: " + obj + "!\n");
      }
    }
    super.setValue(obj);
  }

  @Override
  protected Random parseValue(Object obj) throws ParameterException {
    if(obj instanceof Random) {
      return (Random) obj;
    }
    if(obj instanceof Long) {
      return new Random((Long) obj);
    }
    if(obj instanceof Integer) {
      return new Random((long) (Integer) obj);
    }
    try {
      return new Random(Long.parseLong(obj.toString()));
    }
    catch(NullPointerException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long seed value or a random generator, read: " + obj + "!\n");
    }
    catch(NumberFormatException e) {
      throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getName() + "\" requires a long seed value or a random generator, read: " + obj + "!\n");
    }
  }

  @Override
  public Object getGivenValue() {
    Object r = super.getGivenValue();
    if (r == null && seed != null) {
      super.givenValue = new Random(seed);
      r = super.givenValue;
    }
    return r;
  }

  @Override
  public String getValueAsString() {
    return (seed != null) ? Long.toString(seed) : "null";
  }
}