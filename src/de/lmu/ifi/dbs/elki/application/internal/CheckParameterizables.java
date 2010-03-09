package de.lmu.ifi.dbs.elki.application.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Perform some consistency checks on classes that cannot be specified as Java interface.
 * 
 * @author Erich Schubert
 */
public class CheckParameterizables extends AbstractLoggable {
  public void checkParameterizables() {
    for(final Class<?> cls : InspectionUtil.findAllImplementations(Object.class, false)) {
      final Constructor<?> constructor;
      try {
        constructor = cls.getDeclaredConstructor(Parameterization.class);
      }
      catch(NoClassDefFoundError e) {
        logger.warning("Class discovered but not found?!? "+cls.getName());
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
      try {
        cls.getConstructor(Parameterization.class);
        hasConstructor = true;
      }
      catch(NoClassDefFoundError e) {
        logger.warning("Class discovered but not found?!? "+cls.getName());
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
        logger.warning("Class discovered but not found?!? "+cls.getName());
        // Not found ?!?
        continue;
      }
      catch(Exception e) {
        // do nothing.
      }
      if (!hasConstructor) {
        logger.warning("Class "+cls.getName()+" is Parameterizable but doesn't have a constructor with the appropriate signature!");
      }
    }
  }

  private void checkParameterizable(Class<?> cls, Constructor<?> constructor) {
    if (!Modifier.isPublic(constructor.getModifiers())) {
      logger.warning("Constructor for class "+cls.getName()+" is not public!");
    }
    if (!Parameterizable.class.isAssignableFrom(cls)) {
      logger.warning("Class "+cls.getName()+" should implement Parameterizable!");
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new CheckParameterizables().checkParameterizables();
  }
}
