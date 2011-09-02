package de.lmu.ifi.dbs.elki.application.internal;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Perform some consistency checks on classes that cannot be specified as Java
 * interface.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses Parameterizable
 */
public class CheckParameterizables {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CheckParameterizables.class);

  /**
   * Validate all "Parameterizable" objects for parts of the API contract that
   * cannot be specified in Java interfaces (such as constructors, static
   * methods)
   */
  public void checkParameterizables() {
    LoggingConfiguration.setVerbose(true);
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Object.class, false)) {
      final Constructor<?> constructor;
      try {
        constructor = cls.getDeclaredConstructor(Parameterization.class);
      }
      catch(NoClassDefFoundError e) {
        logger.verbose("Class discovered but not found?!? " + cls.getName());
        // Not found ?!?
        continue;
      }
      catch(Exception e) {
        // Not parameterizable.
        continue;
      }
      checkParameterizable(cls, constructor);
    }
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Parameterizable.class, false)) {
      boolean hasConstructor = false;
      // check for a V3 Parameterizer class
      for(Class<?> inner : cls.getDeclaredClasses()) {
        if(AbstractParameterizer.class.isAssignableFrom(inner)) {
          try {
            Class<? extends AbstractParameterizer> pcls = inner.asSubclass(AbstractParameterizer.class);
            pcls.newInstance();
            if(checkParameterizer(cls, pcls)) {
              hasConstructor = true;
              continue;
            }
          }
          catch(Exception e) {
            logger.verbose("Could not run Parameterizer: " + inner.getName() + ": " + e);
            // continue. Probably non-public
          }
        }
      }

      // check for a V2 factory method.
      try {
        ClassGenericsUtil.getParameterizationFactoryMethod(cls, Object.class);
        hasConstructor = true;
        // logger.debugFine("Found factory method for class: "+ cls.getName());
      }
      catch(NoClassDefFoundError e) {
        logger.verbose("Class discovered but not found?!? " + cls.getName());
        // Not found ?!?
        continue;
      }
      catch(Exception e) {
        // do nothing.
      }
      try {
        cls.getConstructor(Parameterization.class);
        hasConstructor = true;
      }
      catch(NoClassDefFoundError e) {
        logger.verbose("Class discovered but not found?!? " + cls.getName());
        // Not found ?!?
        continue;
      }
      catch(Exception e) {
        // do nothing.
      }
      try {
        cls.getConstructor();
        hasConstructor = true;
      }
      catch(NoClassDefFoundError e) {
        logger.verbose("Class discovered but not found?!? " + cls.getName());
        // Not found ?!?
        continue;
      }
      catch(Exception e) {
        // do nothing.
      }
      if(!hasConstructor) {
        logger.verbose("Class " + cls.getName() + " is Parameterizable but doesn't have a constructor with the appropriate signature!");
      }
    }
  }

  private boolean checkParameterizer(Class<?> cls, Class<? extends AbstractParameterizer> par) {
    try {
      par.getConstructor();
      boolean hasMakeInstance = false;
      final Method methods[] = par.getDeclaredMethods();
      for(int i = 0; i < methods.length; ++i) {
        final Method meth = methods[i];
        if(meth.getName().equals("makeInstance")) {
          // Check for empty signature
          if(meth.getParameterTypes().length == 0) {
            // And check for proper return type.
            if(cls.isAssignableFrom(meth.getReturnType())) {
              hasMakeInstance = true;
            }
          }
        }
      }
      if(hasMakeInstance) {
        return true;
      }
    }
    catch(Exception e) {
      logger.warning("No proper Parameterizer.makeInstance for " + cls.getName() + ": " + e);
      return false;
    }
    logger.warning("No proper Parameterizer.makeInstance for " + cls.getName() + " found!");
    return false;
  }

  private void checkParameterizable(Class<?> cls, Constructor<?> constructor) {
    // Classes in the same package are special and don't cause warnings.
    if(!cls.getName().startsWith(Parameterizable.class.getPackage().getName())) {
      if(!Modifier.isPublic(constructor.getModifiers())) {
        logger.verbose("Constructor for class " + cls.getName() + " is not public!");
      }
      if(!Parameterizable.class.isAssignableFrom(cls)) {
        logger.verbose("Class " + cls.getName() + " should implement Parameterizable!");
      }
    }
  }

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    new CheckParameterizables().checkParameterizables();
  }
}