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
package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Base {@link LogRecord} class used in ELKI.
 * 
 * In contrast to the 'original' LogRecord class, this class will ignore
 * additional classes when determining the 'origin' of a log message.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class ELKILogRecord extends LogRecord {
  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 2820476270420700176L;

  /**
   * Flag whether we still need to infer the caller.
   */
  private transient boolean needToInferCaller = false;

  /**
   * Classes to ignore when finding the relevant caller.
   */
  public static final String[] IGNORE_CLASSES = { //
      Logger.class.getCanonicalName(), //
      Logging.class.getCanonicalName(), //
      LoggingUtil.class.getCanonicalName(), //
      ELKILogRecord.class.getCanonicalName(), //
      // Avoid dependencies on these packages:
      "de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.AbstractParameterization", //
      "de.lmu.ifi.dbs.elki.application.AbstractApplication", //
  };

  /**
   * Name of this class.
   */
  private static final String START_TRACE_AT = Logger.class.getCanonicalName();

  /**
   * Constructor.
   * 
   * @param level Message level
   * @param msg Message contents.
   */
  public ELKILogRecord(Level level, CharSequence msg) {
    super(level, (msg != null) ? msg.toString() : null);
    needToInferCaller = true;
  }

  /*
   * use our inferCaller implementation.
   */
  @Override
  public String getSourceClassName() {
    if(needToInferCaller) {
      inferCallerELKI();
    }
    return super.getSourceClassName();
  }

  /*
   * use our inferCaller implementation.
   */
  @Override
  public void setSourceClassName(String sourceClassName) {
    super.setSourceClassName(sourceClassName);
    needToInferCaller = false;
  }

  /*
   * use our inferCaller implementation.
   */
  @Override
  public String getSourceMethodName() {
    if(needToInferCaller) {
      inferCallerELKI();
    }
    return super.getSourceMethodName();
  }

  /*
   * use our inferCaller implementation.
   */
  @Override
  public void setSourceMethodName(String sourceMethodName) {
    super.setSourceMethodName(sourceMethodName);
    needToInferCaller = false;
  }

  /**
   * Infer a caller, ignoring logging-related classes.
   */
  private void inferCallerELKI() {
    needToInferCaller = false;
    StackTraceElement[] stack = (new Throwable()).getStackTrace();
    int ix = 0;
    // skip back to the logger.
    while(ix < stack.length) {
      StackTraceElement frame = stack[ix];
      final String cls = frame.getClassName();
      if(cls.equals(START_TRACE_AT)) {
        break;
      }
      ix++;
    }
    // skip further back through helper functions
    while(ix < stack.length) {
      StackTraceElement frame = stack[ix];
      final String cls = frame.getClassName();

      boolean ignore = false;
      for(int i = 0; i < IGNORE_CLASSES.length; i++) {
        if(cls.equals(IGNORE_CLASSES[i])) {
          ignore = true;
          break;
        }
      }
      if(!ignore) {
        super.setSourceClassName(frame.getClassName());
        super.setSourceMethodName(frame.getMethodName());
        break;
      }
      ix++;
    }
  }
}
