package de.lmu.ifi.dbs.elki.logging;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.StringParameter;

/**
 * This final class contains some static convenience methods for logging.
 * 
 * {@link #logExpensive} allows the programmer to easily emit a log message,
 * however the function is rather expensive and thus should not be used within
 * loop constructs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ELKILogRecord oneway - - «create»
 */
public final class LoggingUtil {
  /**
   * Expensive logging function that is convenient, but should only be used in
   * rare conditions.
   * 
   * For 'frequent' logging, use more efficient techniques, such as explained in
   * the {@link de.lmu.ifi.dbs.elki.logging logging package documentation}.
   * 
   * @param level Logging level
   * @param message Message to log.
   * @param e Exception to report.
   */
  public static final void logExpensive(Level level, String message, Throwable e) {
    String[] caller = inferCaller();
    if(caller != null) {
      Logger logger = Logger.getLogger(caller[0]);
      logger.logp(level, caller[0], caller[1], message, e);
    }
    else {
      Logger.getAnonymousLogger().log(level, message, e);
    }
  }

  /**
   * Expensive logging function that is convenient, but should only be used in
   * rare conditions.
   * 
   * For 'frequent' logging, use more efficient techniques, such as explained in
   * the {@link de.lmu.ifi.dbs.elki.logging logging package documentation}.
   * 
   * @param level Logging level
   * @param message Message to log.
   */
  public static final void logExpensive(Level level, String message) {
    LogRecord rec = new ELKILogRecord(level, message);
    String[] caller = inferCaller();
    if(caller != null) {
      rec.setSourceClassName(caller[0]);
      rec.setSourceMethodName(caller[1]);
      Logger logger = Logger.getLogger(caller[0]);
      logger.log(rec);
    }
    else {
      Logger.getAnonymousLogger().log(rec);
    }
  }

  /**
   * Static version to log a severe exception.
   * 
   * @param e Exception to log
   */
  public static final void exception(Throwable e) {
    logExpensive(Level.SEVERE, e.getMessage(), e);
  }

  /**
   * Static version to log a severe exception.
   * 
   * @param message Exception message, may be null (defaults to e.getMessage())
   * @param e causing exception
   */
  public static final void exception(String message, Throwable e) {
    if(message == null && e != null) {
      message = e.getMessage();
    }
    logExpensive(Level.SEVERE, message, e);
  }

  /**
   * Static version to log a warning message.
   * 
   * @param message Warning message.
   */
  public static final void warning(String message) {
    logExpensive(Level.WARNING, message);
  }

  /**
   * Static version to log a warning message.
   * 
   * @param message Warning message, may be null (defaults to e.getMessage())
   * @param e causing exception
   */
  public static final void warning(String message, Throwable e) {
    if(message == null && e != null) {
      message = e.getMessage();
    }
    logExpensive(Level.WARNING, message, e);
  }

  /**
   * Static version to log a 'info' message.
   * 
   * @param message Warning message.
   */
  public static final void message(String message) {
    logExpensive(Level.INFO, message);
  }

  /**
   * Static version to log a 'info' message.
   * 
   * @param message Warning message, may be null (defaults to e.getMessage())
   * @param e causing exception
   */
  public static final void message(String message, Throwable e) {
    if(message == null && e != null) {
      message = e.getMessage();
    }
    logExpensive(Level.INFO, message, e);
  }

  /**
   * Infer which class has called the logging helper.
   * 
   * While this looks like duplicated code from ELKILogRecord, it is needed here
   * to find an appropriate Logger (and check the logging level) for the calling
   * class, not just to log the right class and method name.
   * 
   * @return calling class name and calling method name
   */
  private final static String[] inferCaller() {
    StackTraceElement stack[] = (new Throwable()).getStackTrace();
    int ix = 0;
    while(ix < stack.length) {
      StackTraceElement frame = stack[ix];

      if(!frame.getClassName().equals(LoggingUtil.class.getCanonicalName())) {
        return new String[] { frame.getClassName(), frame.getMethodName() };
      }
      ix++;
    }

    return null;
  }

  /**
   * Parse the option string to configure logging.
   * 
   * @param param Parameter to process.
   * 
   * @throws WrongParameterValueException On parsing errors
   */
  public static final void parseDebugParameter(StringParameter param) throws WrongParameterValueException {
    String[] opts = param.getValue().split(",");
    for(String opt : opts) {
      try {
        String[] chunks = opt.split("=");
        if(chunks.length == 1) {
          try {
            Level level = Level.parse(chunks[0]);
            LoggingConfiguration.setDefaultLevel(level);
          }
          catch(IllegalArgumentException e) {
            LoggingConfiguration.setLevelFor(chunks[0], Level.FINEST.getName());
          }
        }
        else if(chunks.length == 2) {
          LoggingConfiguration.setLevelFor(chunks[0], chunks[1]);
        }
        else {
          throw new WrongParameterValueException(param, param.getValue(), "More than one '=' in debug parameter.");
        }
      }
      catch(IllegalArgumentException e) {
        throw (new WrongParameterValueException(param, param.getValue(), "Could not process value.", e));
      }
    }
  }
}