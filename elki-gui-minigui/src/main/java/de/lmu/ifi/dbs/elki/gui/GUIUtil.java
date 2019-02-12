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
package de.lmu.ifi.dbs.elki.gui;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * GUI utilities.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public final class GUIUtil {
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
    try {
      if(PREFER_GTK) {
        LookAndFeelInfo[] lfs = UIManager.getInstalledLookAndFeels();
        for(LookAndFeelInfo lf : lfs) {
          if(lf.getClassName().contains("GTK")) {
            UIManager.setLookAndFeel(lf.getClassName());
            return;
          }
        }
      }
      // Fallback:
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
  public static void logUncaughtExceptions(Logging logger) {
    try {
      Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.exception(e));
    }
    catch(SecurityException e) {
      logger.warning("Could not set the Default Uncaught Exception Handler", e);
    }
  }
}
