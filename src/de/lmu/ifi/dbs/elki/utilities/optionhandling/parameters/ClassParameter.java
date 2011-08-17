package de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.properties.IterateKnownImplementations;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
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
 * @apiviz.uses Properties
 * @apiviz.uses IterateKnownImplementations
 * 
 * @param <C> Class type
 */
// TODO: add additional constructors with parameter constraints.
// TODO: turn restrictionClass into a constraint?
public class ClassParameter<C> extends Parameter<Class<?>, Class<? extends C>> {
  public static final String FACTORY_POSTFIX = "$Factory";

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

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected Class<? extends C> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
    }
    if(obj instanceof Class<?>) {
      return (Class<? extends C>) obj;
    }
    if(obj instanceof String) {
      String value = (String) obj;
      try {
        // Try exact class factory first.
        try {
          return (Class<? extends C>) Class.forName(value + FACTORY_POSTFIX);
        }
        catch(ClassNotFoundException e) {
          // Ignore, retry
        }
        try {
          return (Class<? extends C>) Class.forName(value);
        }
        catch(ClassNotFoundException e) {
          // Ignore, retry
        }
        // Try factory for guessed name next
        try {
          return (Class<? extends C>) Class.forName(restrictionClass.getPackage().getName() + "." + value + FACTORY_POSTFIX);
        }
        catch(ClassNotFoundException e) {
          // Ignore, retry
        }
        // Last try: guessed name prefix only
        return (Class<? extends C>) Class.forName(restrictionClass.getPackage().getName() + "." + value);
      }
      catch(ClassNotFoundException e) {
        throw new WrongParameterValueException(this, value, "Given class \"" + value + "\" not found.", e);
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
      throw new UnspecifiedParameterException("Parameter Error.\n" + "No value for parameter \"" + getName() + "\" " + "given.");
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
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter#hasValuesDescription()
   */
  @Override
  public boolean hasValuesDescription() {
    return restrictionClass != null && restrictionClass != Object.class;
  }

  /**
   * Return a description of known valid classes.
   * 
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter#getValuesDescription()
   */
  @Override
  public String getValuesDescription() {
    if(restrictionClass != null && restrictionClass != Object.class) {
      return restrictionString();
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
        // inner exception during instantiation. Log, so we don't lose it!
        LoggingUtil.exception(e);
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class.", e);
      }
      catch(NoSuchMethodException e) {
        throw new WrongParameterValueException(this, getValue().getCanonicalName(), "Error instantiating class - no usable public constructor.");
      }
      catch(Exception e) {
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
   * @return {@link java.lang.Iterable Iterable} and {@link java.util.Iterator
   *         Iterator} object
   */
  public IterableIterator<Class<?>> getKnownImplementations() {
    if(InspectionUtil.NONSTATIC_CLASSPATH) {
      return IterableUtil.fromIterable(InspectionUtil.cachedFindAllImplementations(getRestrictionClass()));
    }
    return new IterateKnownImplementations(getRestrictionClass());
  }

  /**
   * Returns the class names allowed according to the restriction class of this
   * class parameter.
   * 
   * @return class names allowed according to the restriction class defined.
   */
  public String[] getRestrictionClasses() {
    if(restrictionClass != null) {
      return Properties.ELKI_PROPERTIES.getProperty(restrictionClass.getName());
    }
    return new String[] {};
  }

  /**
   * Provides a description string listing all classes for the given superclass
   * or interface as specified in the properties.
   * 
   * @return a description string listing all classes for the given superclass
   *         or interface as specified in the properties
   */
  public String restrictionString() {
    StringBuilder info = new StringBuilder();
    if(restrictionClass.isInterface()) {
      info.append("Implementing ");
    }
    else {
      info.append("Extending ");
    }
    info.append(restrictionClass.getName());
    info.append(FormatUtil.NEWLINE);

    IterableIterator<Class<?>> known = getKnownImplementations();
    if(known.hasNext()) {
      info.append("Known classes (default package " + restrictionClass.getPackage().getName() + "):");
      info.append(FormatUtil.NEWLINE);
      for(Class<?> c : known) {
        info.append("->" + FormatUtil.NONBREAKING_SPACE);
        info.append(canonicalClassName(c, getRestrictionClass()));
        info.append(FormatUtil.NEWLINE);
      }
    }
    return info.toString();
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
      return canonicalClassName(c, null, FACTORY_POSTFIX);
    }
    return canonicalClassName(c, parent.getPackage(), FACTORY_POSTFIX);
  }

  @Override
  public String getDefaultValueAsString() {
    return canonicalClassName(getDefaultValue(), getRestrictionClass());
  }
}