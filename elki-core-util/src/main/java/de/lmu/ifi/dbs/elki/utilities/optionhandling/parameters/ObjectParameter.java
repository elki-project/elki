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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter representing a single object.
 * 
 * It can be parameterized by giving a class name or class to instantiate, or an
 * existing instance.
 * 
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.3
 * @param <C> Class type
 */
public class ObjectParameter<C> extends ClassParameter<C> {
  /**
   * The instance to use.
   */
  private C instance;

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default value of this class parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass, Class<?> defaultValue) {
    super(optionID, restrictionClass, defaultValue);
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   * 
   * @param <T> default value type, to solve generics problems.
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default instance of this class parameter
   */
  public <T extends C> ObjectParameter(OptionID optionID, Class<?> restrictionClass, T defaultValue) {
    super(optionID, restrictionClass);
    this.instance = defaultValue;
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and optional flag.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, restrictionClass, optional);
  }

  /**
   * Constructs a class parameter with the given optionID, and restriction
   * class.
   * 
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   */
  public ObjectParameter(OptionID optionID, Class<?> restrictionClass) {
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    super(optionID, restrictionClass);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<? extends C> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    // does the given objects class fit?
    if(restrictionClass.isInstance(obj)) {
      return (Class<? extends C>) obj.getClass();
    }
    return super.parseValue(obj);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setValue(Object obj) throws ParameterException {
    // This is a bit hackish. But when given an appropriate instance, keep it.
    if(restrictionClass.isInstance(obj)) {
      instance = (C) obj;
    }
    super.setValue(obj);
  }

  /**
   * Returns a string representation of the parameter's type.
   * 
   * @return &quot;&lt;class&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<class|object>";
  }

  /**
   * Returns a new instance for the value (i.e., the class name) of this class
   * parameter. The instance has the type of the restriction class of this class
   * parameter.
   * <p>
   * If the Class for the class name is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.F
   * 
   * @param config Parameterization
   * @return a new instance for the value of this class parameter
   */
  @Override
  public C instantiateClass(Parameterization config) {
    return (instance != null) ? instance : (instance = super.instantiateClass(config));
    // NOTE: instance may remain null here, when instantiateClass failed.
  }
}
