package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a class name.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 *
 * @apiviz.uses InspectionUtil
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
      LoggingUtil.warning("Restriction class 'null' for parameter '" + optionID + "'", new Throwable());
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
      LoggingUtil.warning("Restriction class 'null' for parameter '" + optionID + "'", new Throwable());
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
    if(!super.validate(obj)) {
      return false;
    }
    return true;
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

  /**
   * This class sometimes provides a list of value descriptions.
   *
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.AbstractParameter#hasValuesDescription()
   */
  @Override
  public boolean hasValuesDescription() {
    return restrictionClass != null && restrictionClass != Object.class;
  }

  /**
   * Return a description of known valid classes.
   *
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.AbstractParameter#getValuesDescription()
   */
  @Override
  public String getValuesDescription() {
    if(restrictionClass != null && restrictionClass != Object.class) {
      StringBuilder info = new StringBuilder();
      if(restrictionClass.isInterface()) {
        info.append("Implementing ");
      }
      else {
        info.append("Extending ");
      }
      info.append(restrictionClass.getName());
      info.append(FormatUtil.NEWLINE);

      List<Class<?>> known = getKnownImplementations();
      if(!known.isEmpty()) {
        info.append("Known classes (default package " + restrictionClass.getPackage().getName() + "):");
        info.append(FormatUtil.NEWLINE);
        for(Class<?> c : known) {
          info.append("->").append(FormatUtil.NONBREAKING_SPACE);
          info.append(canonicalClassName(c, getRestrictionClass()));
          info.append(FormatUtil.NEWLINE);
        }
      }
      return info.toString();
    }
    return "";
  }

  @Override
  public String getValueAsString() {
    return canonicalClassName(getValue(), getRestrictionClass());
  }

  /**
   * Returns a new instance for the value (i.e., the class name) of this class
   * parameter. The instance has the type of the restriction class of this class
   * parameter.
   * <p/>
   * If the Class for the class name is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.
   *
   * @param config Parameterization to use (if Parameterizable))
   * @return a new instance for the value of this class parameter
   */
  public C instantiateClass(Parameterization config) {
    try {
      if(getValue() == null /* && !optionalParameter */) {
        throw new UnusedParameterException("Value of parameter " + getName() + " has not been specified.");
      }
      C instance;
      try {
        config = config.descend(this);
        instance = ClassGenericsUtil.tryInstantiate(restrictionClass, getValue(), config);
      }
      catch(InvocationTargetException e) {
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e);
      }
      catch(NoSuchMethodException e) {
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class - no usable public constructor.");
      }
      catch(Exception e) {
        if(LOG.isVerbose()) {
          LOG.exception("Class initialization failed.", e);
        }
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e);
      }
      return instance;
    }
    catch(ParameterException e) {
      config.reportError(e);
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

  /**
   * Get the "simple" form of a class name.
   *
   * @param c Class
   * @param pkg Package
   * @param postfix Postfix to strip
   *
   * @return Simplified class name
   */
  public static String canonicalClassName(Class<?> c, Package pkg, String postfix) {
    String name = c.getName();
    if(pkg != null) {
      String prefix = pkg.getName() + ".";
      if(name.startsWith(prefix)) {
        name = name.substring(prefix.length());
      }
    }
    if(postfix != null && name.endsWith(postfix)) {
      name = name.substring(0, name.length() - postfix.length());
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
    if(parent == null) {
      return canonicalClassName(c, null, ELKIServiceRegistry.FACTORY_POSTFIX);
    }
    return canonicalClassName(c, parent.getPackage(), ELKIServiceRegistry.FACTORY_POSTFIX);
  }

  @Override
  public String getDefaultValueAsString() {
    return canonicalClassName(getDefaultValue(), getRestrictionClass());
  }
}
