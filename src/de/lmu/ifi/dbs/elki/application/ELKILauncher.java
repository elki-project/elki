package de.lmu.ifi.dbs.elki.application;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.lang.reflect.Method;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;

/**
 * Class to launch ELKI.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses AbstractApplication
 */
public class ELKILauncher {
  /**
   * Application to run by default.
   */
  public static final Class<? extends AbstractApplication> DEFAULT_APPLICATION = MiniGUI.class;

  /**
   * Launch ELKI.
   * 
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    if (args.length > 0 && args[0].charAt(0) != '-') {
      try {
        Class<?> cls = findMainClass(args[0]);
        Method m = cls.getMethod("main", String[].class);
        Object a = Arrays.copyOfRange(args, 1, args.length);
        try {
          m.invoke(null, a);
        } catch (Exception e) {
          LoggingUtil.exception(e);
        }
        return;
      } catch (Exception e) {
        // Ignore
      }
    }
    try {
      Method m = DEFAULT_APPLICATION.getMethod("main", String[].class);
      m.invoke(null, (Object) args);
    } catch (Exception e) {
      LoggingUtil.exception(e);
    }
  }

  /**
   * Find a class for the given name.
   * 
   * @param name Class name
   * @return Class
   * @throws ClassNotFoundException
   */
  private static Class<?> findMainClass(String name) throws ClassNotFoundException {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      // pass
    }
    try {
      return Class.forName(AbstractApplication.class.getPackage().getName() + '.' + name);
    } catch (ClassNotFoundException e) {
      // pass
    }
    for (Class<?> c : InspectionUtil.cachedFindAllImplementations(AbstractApplication.class)) {
      if (c.isAnnotationPresent(Alias.class)) {
        Alias aliases = c.getAnnotation(Alias.class);
        for (String alias : aliases.value()) {
          if (alias.equalsIgnoreCase(name)) {
            return c;
          }
        }
      }
    }
    throw new ClassNotFoundException(name);
  }
}
