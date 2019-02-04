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
package de.lmu.ifi.dbs.elki.application.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.Logging.Level;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceLoader;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Perform some consistency checks on classes that cannot be specified as Java
 * interface.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - - - AbstractParameterizer
 * @has - - - State
 */
public class CheckParameterizables {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CheckParameterizables.class);

  /**
   * Known parameterizable classes/interfaces.
   */
  private List<Class<?>> knownParameterizables;

  /**
   * Validate all "Parameterizable" objects for parts of the API contract that
   * cannot be specified in Java interfaces (such as constructors, static
   * methods)
   */
  public void checkParameterizables() {
    LoggingConfiguration.setVerbose(Level.VERBOSE);
    knownParameterizables = new ArrayList<>();
    try {
      Enumeration<URL> us = getClass().getClassLoader().getResources(ELKIServiceLoader.RESOURCE_PREFIX);
      while(us.hasMoreElements()) {
        URL u = us.nextElement();
        if("file".equals(u.getProtocol())) {
          for(String prop : new File(u.toURI()).list()) {
            try {
              knownParameterizables.add(Class.forName(prop));
            }
            catch(ClassNotFoundException e) {
              LOG.warning("Service file name is not a class name: " + prop);
              continue;
            }
          }
        }
        else if(("jar".equals(u.getProtocol()))) {
          JarURLConnection con = (JarURLConnection) u.openConnection();
          try (JarFile jar = con.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while(entries.hasMoreElements()) {
              String prop = entries.nextElement().getName();
              if(prop.startsWith(ELKIServiceLoader.RESOURCE_PREFIX)) {
                prop = prop.substring(ELKIServiceLoader.RESOURCE_PREFIX.length());
              }
              else if(prop.startsWith(ELKIServiceLoader.FILENAME_PREFIX)) {
                prop = prop.substring(ELKIServiceLoader.FILENAME_PREFIX.length());
              }
              else {
                continue;
              }
              try {
                knownParameterizables.add(Class.forName(prop));
              }
              catch(ClassNotFoundException e) {
                LOG.warning("Service file name is not a class name: " + prop);
                continue;
              }
            }
          }
        }
      }
    }
    catch(IOException | URISyntaxException e) {
      throw new AbortException("Error enumerating service folders.", e);
    }

    final String internal = de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizer.class.getPackage().getName();
    for(final Class<?> cls : ELKIServiceRegistry.findAllImplementations(Object.class, false, false)) {
      // Classes in the same package are special and don't cause warnings.
      if(cls.getName().startsWith(internal)) {
        continue;
      }
      try {
        State state = State.NO_CONSTRUCTOR;
        state = checkV3Parameterization(cls, state);
        if(state == State.ERROR) {
          continue;
        }
        state = checkDefaultConstructor(cls, state);
        if(state == State.ERROR) {
          continue;
        }
        boolean expectedParameterizer = checkSupertypes(cls);
        if(state == State.NO_CONSTRUCTOR && expectedParameterizer) {
          LOG.verbose("Class " + cls.getName() + //
          " implements a parameterizable interface, but doesn't have a public and parameterless constructor!");
        }
        if(state == State.INSTANTIABLE && !expectedParameterizer) {
          LOG.verbose("Class " + cls.getName() + //
          " has a parameterizer, but there is no service file for any of its interfaces.");
        }
      }
      catch(NoClassDefFoundError e) {
        LOG.verbose("Class discovered but not found: " + cls.getName() + " (missing: " + e.getMessage() + ")");
      }
    }
  }

  /**
   * Check all supertypes of a class.
   *
   * @param cls Class to check.
   * @return {@code true} when at least one supertype is a known parameterizable
   *         type.
   */
  private boolean checkSupertypes(Class<?> cls) {
    for(Class<?> c : knownParameterizables) {
      if(c.isAssignableFrom(cls)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Current verification state.
   *
   * @author Erich Schubert
   */
  enum State {
    NO_CONSTRUCTOR, //
    INSTANTIABLE, //
    DEFAULT_INSTANTIABLE, //
    ERROR, //
  }

  /** Check for a V3 constructor. */
  private State checkV3Parameterization(Class<?> cls, State state) throws NoClassDefFoundError {
    // check for a V3 Parameterizer class
    for(Class<?> inner : cls.getDeclaredClasses()) {
      if(AbstractParameterizer.class.isAssignableFrom(inner)) {
        try {
          Class<? extends AbstractParameterizer> pcls = inner.asSubclass(AbstractParameterizer.class);
          pcls.newInstance();
          if(checkParameterizer(cls, pcls)) {
            if(state == State.INSTANTIABLE) {
              LOG.warning("More than one parameterization method in class " + cls.getName());
            }
            state = State.INSTANTIABLE;
          }
        }
        catch(Exception|Error e) {
          LOG.verbose("Could not run Parameterizer: " + inner.getName() + ": " + e.getMessage());
          // continue. Probably non-public
        }
      }
    }
    return state;
  }

  /** Check for a default constructor. */
  private State checkDefaultConstructor(Class<?> cls, State state) throws NoClassDefFoundError {
    try {
      cls.getConstructor();
      return State.DEFAULT_INSTANTIABLE;
    }
    catch(Exception e) {
      // do nothing.
    }
    return state;
  }

  private boolean checkParameterizer(Class<?> cls, Class<? extends AbstractParameterizer> par) {
    int checkResult = 0;
    try {
      par.getConstructor();
      final Method[] methods = par.getDeclaredMethods();
      for(int i = 0; i < methods.length; ++i) {
        final Method meth = methods[i];
        if(meth.getName().equals("makeInstance")) {
          // Check for empty signature
          if(meth.getParameterTypes().length == 0) {
            // And check for proper return type.
            if(cls.isAssignableFrom(meth.getReturnType())) {
              checkResult = 1;
            }
            else if(checkResult == 0) {
              checkResult = 2; // Nothing better
            }
          }
          else if(checkResult == 0) {
            checkResult += 3;
          }
        }
      }
    }
    catch(Exception e) {
      LOG.warning("No proper Parameterizer.makeInstance for " + cls.getName() + ": " + e);
      return false;
    }
    if(checkResult > 1) {
      LOG.warning("No proper Parameterizer.makeInstance for " + cls.getName() + " found!");
    }
    return checkResult == 1;
  }

  /**
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    new CheckParameterizables().checkParameterizables();
  }
}