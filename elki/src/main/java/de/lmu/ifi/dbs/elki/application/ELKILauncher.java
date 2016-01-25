package de.lmu.ifi.dbs.elki.application;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.lang.reflect.Method;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;

/**
 * Class to launch ELKI.
 *
 * @author Erich Schubert
 * @since 0.5.5
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
    if(args.length > 0 && args[0].charAt(0) != '-') {
      Class<?> cls = ELKIServiceRegistry.findImplementation(AbstractApplication.class, args[0]);
      if(cls != null) {
        try {
          Method m = cls.getMethod("main", String[].class);
          Object a = Arrays.copyOfRange(args, 1, args.length);
          m.invoke(null, a);
        }
        catch(InvocationTargetException e) {
          LoggingUtil.exception(e.getCause());
        }
        catch(Exception e) {
          LoggingUtil.exception(e);
        }
        return;
      }
    }
    try {
      Method m = DEFAULT_APPLICATION.getMethod("main", String[].class);
      m.invoke(null, (Object) args);
    }
    catch(Exception e) {
      LoggingUtil.exception(e);
    }
  }
}
