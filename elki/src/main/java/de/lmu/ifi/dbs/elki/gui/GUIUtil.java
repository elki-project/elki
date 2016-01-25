package de.lmu.ifi.dbs.elki.gui;

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

import java.awt.Toolkit;
import java.lang.reflect.Method;

import javax.swing.RepaintManager;
import javax.swing.UIManager;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * GUI utilities.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public final class GUIUtil {
  /**
   * Enable thread repaint debugging.
   */
  public static final boolean THREAD_REPAINT_DEBUG = false;

  /**
   * Whether to prefer the GTK look and feel on Unix.
   */
  public static final boolean PREFER_GTK = true;

  /**
   * Fake constructor. Do not instantiate.
   */
  private GUIUtil() {
    // Static methods only - do not instantiate
  }

  /**
   * Setup look at feel.
   */
  public static void setLookAndFeel() {
    // If enabled, setup thread debugging.
    if(THREAD_REPAINT_DEBUG) {
      try {
        Class<?> cls = ClassLoader.getSystemClassLoader().loadClass("org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager");
        RepaintManager.setCurrentManager((RepaintManager) cls.newInstance());
      }
      catch(Exception e) {
        // ignore
      }
    }
    if(PREFER_GTK) {
      try {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        // Note: we don't want to *require* these classes
        // But if they exist, we're going to try using them.
        Class<?> suntoolkit = Class.forName("sun.awt.SunToolkit");
        Method testm = suntoolkit.getMethod("isNativeGTKAvailable");
        if(suntoolkit.isInstance(toolkit) && (Boolean) testm.invoke(toolkit)) {
          UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
          return;
        }
      }
      catch(Exception e) {
        // ignore
      }
    }
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      // ignore
    }
  }

  /**
   * Setup logging of uncaught exceptions.
   * 
   * @param logger logger
   */
  public static void logUncaughtExceptions(final Logging logger) {
    try {
      Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
          logger.exception(e);
        }
      });
    }
    catch(SecurityException e) {
      logger.warning("Could not set the Default Uncaught Exception Handler", e);
    }
  }
}
