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

import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a class name.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 *
 * @assoc - - - ELKIServiceRegistry
 *
 * @param <C> Class type
 */
// TODO: add additional constructors with parameter constraints.
// TODO: turn restrictionClass into a constraint?
public class ClassParameter<C> extends AbstractParameter<ClassParameter<C>, Class<? extends C>> {
  /**
   * The class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClassParameter.class);

  /**
   * The restriction class for this class parameter.
   */
  protected Class<C> restrictionClass;

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and default value.
   *
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param defaultValue the default value of this class parameter
   */
  @SuppressWarnings("unchecked")
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, Class<?> defaultValue) {
    super(optionID, (Class<? extends C>) defaultValue);
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    this.restrictionClass = (Class<C>) restrictionClass;
    if(restrictionClass == null) {
      LOG.warning("Restriction class 'null' for parameter '" + optionID + "'", new Throwable());
    }
  }

  /**
   * Constructs a class parameter with the given optionID, restriction class,
   * and optional flag.
   *
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   * @param optional specifies if this parameter is an optional parameter
   */
  @SuppressWarnings("unchecked")
  public ClassParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, optional);
    // It would be nice to be able to use Class<C> here, but this won't work
    // with nested Generics:
    // * ClassParameter<Foo<Bar>>(optionID, Foo.class) doesn't satisfy Class<C>
    // * ClassParameter<Foo<Bar>>(optionID, Foo<Bar>.class) isn't valid
    // * ClassParameter<Foo<Bar>>(optionID, (Class<Foo<Bar>>) Foo.class) is an
    // invalid cast.
    this.restrictionClass = (Class<C>) restrictionClass;
    if(restrictionClass == null) {
      LOG.warning("Restriction class 'null' for parameter '" + optionID + "'", new Throwable());
    }
  }

  /**
   * Constructs a class parameter with the given optionID, and restriction
   * class.
   *
   * @param optionID the unique id of the option
   * @param restrictionClass the restriction class of this class parameter
   */
  public ClassParameter(OptionID optionID, Class<?> restrictionClass) {
    this(optionID, restrictionClass, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<? extends C> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(obj instanceof Class<?>) {
      return (Class<? extends C>) obj;
    }
    if(obj instanceof String) {
      Class<? extends C> clz = ELKIServiceRegistry.findImplementation(restrictionClass, (String) obj);
      if(clz != null) {
        return clz;
      }
    }
    throw new WrongParameterValueException(this, obj.toString(), "Class not found for given value. Must be a subclass / implementation of " + restrictionClass.getName());
  }

  /**
   * Checks if the given parameter value is valid for this ClassParameter. If
   * not a parameter exception is thrown.
   */
  @Override
  public boolean validate(Class<? extends C> obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(!restrictionClass.isAssignableFrom(obj)) {
      throw new WrongParameterValueException(this, obj.getName(), "Given class not a subclass / implementation of " + restrictionClass.getName());
    }
    return super.validate(obj);
  }

  /**
   * Returns a string representation of the parameter's type.
   *
   * @return &quot;&lt;class&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<class>";
  }

  @Override
  public StringBuilder describeValues(StringBuilder info) {
    if(restrictionClass == null || restrictionClass == Object.class) {
      return info;
    }
    info.append(restrictionClass.isInterface() ? "Implementing " : "Extending ") //
        .append(restrictionClass.getName()) //
        .append(FormatUtil.NEWLINE);

    List<Class<?>> known = getKnownImplementations();
    if(!known.isEmpty()) {
      for(Class<?> c : known) {
        info.append("->").append(FormatUtil.NONBREAKING_SPACE) //
            .append(canonicalClassName(c, getRestrictionClass())) //
            .append(FormatUtil.NEWLINE);
      }
    }
    return info;
  }

  @Override
  public String getValueAsString() {
    return canonicalClassName(getValue(), getRestrictionClass());
  }

  @Override
  public String getDefaultValueAsString() {
    return canonicalClassName(getDefaultValue(), getRestrictionClass());
  }

  /**
   * Get the "simple" form of a class name.
   *
   * @param c Class
   * @param pkg Package
   *
   * @return Simplified class name
   */
  public static String canonicalClassName(Class<?> c, Package pkg) {
    String name = c.getName();
    if(pkg != null) {
      final String pkgname = pkg.getName();
      if(name.length() > pkgname.length() && name.startsWith(pkgname) && name.charAt(pkgname.length()) == '.') {
        name = name.substring(pkgname.length() + 1);
      }
    }
    if(name.endsWith(ELKIServiceRegistry.FACTORY_POSTFIX)) {
      name = name.substring(0, name.length() - ELKIServiceRegistry.FACTORY_POSTFIX.length());
    }
    return name;
  }

  /**
   * Get the "simple" form of a class name.
   *
   * @param c Class name
   * @param parent Parent/restriction class (to get package name to strip)
   * @return Simplified class name.
   */
  public static String canonicalClassName(Class<?> c, Class<?> parent) {
    return canonicalClassName(c, parent == null ? null : parent.getPackage());
  }

  /**
   * Returns a new instance for the value (i.e., the class name) of this class
   * parameter. The instance has the type of the restriction class of this class
   * parameter.
   * <p>
   * If the Class for the class name is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.
   *
   * @param config Parameterization to use (if Parameterizable))
   * @return a new instance for the value of this class parameter
   */
  public C instantiateClass(Parameterization config) {
    if(getValue() == null /* && !optionalParameter */) {
      config.reportError(new UnspecifiedParameterException(this));
      return null;
    }
    try {
      config = config.descend(this);
      return ClassGenericsUtil.tryInstantiate(restrictionClass, getValue(), config);
    }
    catch(ClassInstantiationException e) {
      config.reportError(new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e));
      return null;
    }
  }

  /**
   * Returns the restriction class of this class parameter.
   *
   * @return the restriction class of this class parameter.
   */
  public Class<C> getRestrictionClass() {
    return restrictionClass;
  }

  /**
   * Get an iterator over all known implementations of the class restriction.
   *
   * @return List object
   */
  public List<Class<?>> getKnownImplementations() {
    return ELKIServiceRegistry.findAllImplementations(getRestrictionClass());
  }
}
