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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter class for a parameter specifying a list of class names.
 *
 * @author Steffi Wanka
 * @author Erich Schubert
 * @since 0.1
 *
 * @assoc - - - ELKIServiceRegistry
 *
 * @param <C> Class type
 */
// TODO: Add missing constructors. (ObjectListParameter also!)
public class ClassListParameter<C> extends ListParameter<ClassListParameter<C>, List<Class<? extends C>>> {
  /**
   * The restriction class for the list of class names.
   */
  protected Class<C> restrictionClass;

  /**
   * Constructs a class list parameter with the given optionID and restriction
   * class.
   *
   * @param optionID the unique id of this parameter
   * @param restrictionClass the restriction class of the list of class names
   * @param optional specifies if this parameter is an optional parameter
   */
  @SuppressWarnings("unchecked")
  public ClassListParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, optional);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  /**
   * Constructs a class list parameter with the given optionID and restriction
   * class.
   *
   * @param optionID the unique id of this parameter
   * @param restrictionClass the restriction class of the list of class names
   */
  @SuppressWarnings("unchecked")
  public ClassListParameter(OptionID optionID, Class<?> restrictionClass) {
    super(optionID);
    this.restrictionClass = (Class<C>) restrictionClass;
  }

  @Override
  public String getValueAsString() {
    return formatValue(getValue());
  }

  @Override
  public String getDefaultValueAsString() {
    return formatValue(getDefaultValue());
  }

  /**
   * Format as string.
   * 
   * @param val Value to format
   * @return String
   */
  protected String formatValue(List<Class<? extends C>> val) {
    StringBuilder buf = new StringBuilder(50 + val.size() * 25);
    String pkgname = restrictionClass.getPackage().getName();
    for(Class<? extends C> c : val) {
      if(buf.length() > 0) {
        buf.append(LIST_SEP);
      }
      String name = c.getName();
      boolean stripPrefix = name.length() > pkgname.length() && name.startsWith(pkgname) && name.charAt(pkgname.length()) == '.';
      buf.append(name, stripPrefix ? pkgname.length() + 1 : 0, name.length());
    }
    return buf.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Class<? extends C>> parseValue(Object obj) throws ParameterException {
    try {
      List<?> l = List.class.cast(obj);
      // do extra validation:
      for(Object o : l) {
        if(!(o instanceof Class)) {
          throw new WrongParameterValueException("Wrong parameter format for parameter \"" + getOptionID().getName() + "\". Given list contains objects of different type!");
        }
      }
      // TODO: can we use reflection to get extra checks?
      // TODO: Should we copy the list?
      return (List<Class<? extends C>>) l;
    }
    catch(ClassCastException e) {
      // continue with others
    }
    // Did we get a single class?
    try {
      if(restrictionClass.isAssignableFrom((Class<?>) obj)) {
        return Arrays.asList((Class<? extends C>) obj);
      }
    }
    catch(ClassCastException e) {
      // continue with others
    }
    if(obj instanceof String) {
      String[] classes = SPLIT.split((String) obj);
      // TODO: allow empty lists (and list constraints) to enforce length?
      if(classes.length == 0) {
        throw new WrongParameterValueException("Wrong parameter format! Given list of classes for parameter \"" + getOptionID().getName() + "\" is either empty or has the wrong format!");
      }

      List<Class<? extends C>> cls = new ArrayList<>(classes.length);
      for(String cl : classes) {
        Class<? extends C> clz = ELKIServiceRegistry.findImplementation(restrictionClass, cl);
        if(clz == null) {
          throw new WrongParameterValueException(this, (String) obj, "Class '" + cl + "' not found for given value. Must be a subclass / implementation of " + restrictionClass.getName());
        }
        cls.add(clz);
      }
      return cls;
    }
    // INCOMPLETE
    throw new WrongParameterValueException("Wrong parameter format! Parameter \"" + getOptionID().getName() + "\" requires a list of Class values!");
  }

  @Override
  protected boolean validate(List<Class<? extends C>> obj) throws ParameterException {
    for(Class<? extends C> cls : obj) {
      if(!restrictionClass.isAssignableFrom(cls)) {
        throw new WrongParameterValueException(this, cls.getName(), "Class \"" + cls.getName() + "\" does not extend/implement restriction class " + restrictionClass + ".\n");
      }
    }
    return super.validate(obj);
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
   * Returns a string representation of the parameter's type.
   *
   * @return &quot;&lt;class_1,...,class_n&gt;&quot;
   */
  @Override
  public String getSyntax() {
    return "<class_1,...,class_n>";
  }

  /**
   * Returns a list of new instances for the value (i.e., the class name) of
   * this class list parameter. The instances have the type of the restriction
   * class of this class list parameter.
   * <p>
   * If the Class for the class names is not found, the instantiation is tried
   * using the package of the restriction class as package of the class name.
   *
   * @param config Parameterization to use (if Parameterizable))
   * @return a list of new instances for the value of this class list parameter
   */
  public List<C> instantiateClasses(Parameterization config) {
    config = config.descend(this);
    List<C> instances = new ArrayList<>();
    if(getValue() == null) {
      config.reportError(new UnspecifiedParameterException(this));
      return instances; // empty list.
    }

    for(Class<? extends C> cls : getValue()) {
      // NOTE: There is a duplication of this code in ObjectListParameter - keep
      // in sync!
      try {
        instances.add(ClassGenericsUtil.tryInstantiate(restrictionClass, cls, config));
      }
      catch(Exception e) {
        config.reportError(new WrongParameterValueException(this, cls.getName(), e.getMessage(), e));
      }
    }
    return instances;
  }

  @Override
  public StringBuilder describeValues(StringBuilder buf) {
    if(restrictionClass == null || restrictionClass == Object.class) {
      return buf;
    }
    buf.append(restrictionClass.isInterface() ? "Implementing " : "Extending ") //
        .append(restrictionClass.getName()) //
        .append(FormatUtil.NEWLINE);

    List<Class<?>> known = getKnownImplementations();
    if(!known.isEmpty()) {
      String pkgName = restrictionClass.getPackage().getName();
      buf.append("Known classes (default package ").append(pkgName).append("):") //
          .append(FormatUtil.NEWLINE);
      for(Class<?> c : known) {
        String name = c.getName();
        final boolean stripPrefix = name.length() > pkgName.length() && name.startsWith(pkgName) && name.charAt(pkgName.length()) == '.';
        buf.append("->").append(FormatUtil.NONBREAKING_SPACE) //
            .append(name, stripPrefix ? pkgName.length() + 1 : 0, name.length()) //
            .append(FormatUtil.NEWLINE);
      }
    }
    return buf;
  }

  @Override
  public int size() {
    return getValue().size();
  }
}
