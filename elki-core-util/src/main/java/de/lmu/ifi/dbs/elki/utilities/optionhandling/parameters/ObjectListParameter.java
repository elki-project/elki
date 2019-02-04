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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnspecifiedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parameter that represents a list of objects (in contrast to a class list
 * parameter, they will be instanced at most once.)
 * 
 * @author Erich Schubert
 * @since 0.3
 *
 * @param <C> Class type
 */
// TODO: add missing constructors.
public class ObjectListParameter<C> extends ClassListParameter<C> {
  /**
   * Cache for the generated instances.
   */
  private List<C> instances = null;

  /**
   * Constructor with optional flag.
   * 
   * @param optionID Option ID
   * @param restrictionClass Restriction class
   * @param optional optional flag
   */
  public ObjectListParameter(OptionID optionID, Class<?> restrictionClass, boolean optional) {
    super(optionID, restrictionClass, optional);
  }

  /**
   * Constructor for non-optional.
   * 
   * @param optionID Option ID
   * @param restrictionClass Restriction class
   */
  public ObjectListParameter(OptionID optionID, Class<?> restrictionClass) {
    super(optionID, restrictionClass);
  }

  @Override
  public String getSyntax() {
    return "<object_1|class_1,...,object_n|class_n>";
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Class<? extends C>> parseValue(Object obj) throws ParameterException {
    if(obj == null) {
      throw new UnspecifiedParameterException(this);
    }
    if(List.class.isInstance(obj)) {
      List<?> l = (List<?>) obj;
      ArrayList<C> inst = new ArrayList<>(l.size());
      ArrayList<Class<? extends C>> classes = new ArrayList<>(l.size());
      for(Object o : l) {
        // does the given objects class fit?
        if(restrictionClass.isInstance(o)) {
          inst.add((C) o);
          classes.add((Class<? extends C>) o.getClass());
        }
        else if(o instanceof Class) {
          if(restrictionClass.isAssignableFrom((Class<?>) o)) {
            inst.add(null);
            classes.add((Class<? extends C>) o);
          }
          else {
            throw new WrongParameterValueException(this, ((Class<?>) o).getName(), "Given class not a subclass / implementation of " + restrictionClass.getName());
          }
        }
        else {
          throw new WrongParameterValueException(this, o.getClass().getName(), "Given instance not an implementation of " + restrictionClass.getName());
        }
      }
      this.instances = inst;
      return super.parseValue(classes);
    }
    // Did we get a single instance?
    try {
      C inst = restrictionClass.cast(obj);
      this.instances = Arrays.asList(inst);
      return super.parseValue(inst.getClass());
    }
    catch(ClassCastException e) {
      // Continue
    }
    return super.parseValue(obj);
  }

  @Override
  public List<C> instantiateClasses(Parameterization config) {
    if(instances == null) {
      // instantiateClasses will descend itself.
      return instances = super.instantiateClasses(config);
    }
    Parameterization cfg = null;
    final List<Class<? extends C>> classes = getValue();
    for(int i = 0; i < classes.size(); i++) {
      if(instances.get(i) == null) {
        Class<? extends C> cls = classes.get(i);
        try {
          // Descend at most once, and only when needed
          cfg = (cfg == null) ? config.descend(this) : cfg;
          instances.set(i, ClassGenericsUtil.tryInstantiate(restrictionClass, cls, cfg));
        }
        catch(Exception e) {
          config.reportError(new WrongParameterValueException(this, cls.getName(), e.getMessage(), e));
        }
      }
    }
    return instances;
  }
}
