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

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;

/**
 * Format a log record for error output, including a stack trace if available.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
// TODO: make more configurable and handle suppressed exceptions
public class ErrorFormatter extends Formatter {
  /**
   * List of packages to prune from stack traces at the end.
   * 
   * TODO: make configurable via logging.properties
   */
  public static final String[] PRUNE = { //
      "de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI", //
      "de.lmu.ifi.dbs.elki.KDDTask", //
      "java.awt.event.", //
      "java.awt.EventDispatchThread", //
      "java.awt.EventQueue", //
      "java.security.", //
      "java.lang.Thread", //
      "java.util.concurrent.", //
      "javax.swing.SwingWorker", //
      "java.util.concurrent.FutureTask", //
      "org.apache.batik.", //
  };

  /**
   * Null error message.
   */
  private final String NULLMSG = "null" + OutputStreamLogger.NEWLINE;

  /**
   * Constructor.
   */
  public ErrorFormatter() {
    super();
  }

  @Override
  public String format(LogRecord record) {
    if(record instanceof ProgressLogRecord) {
      return record.getMessage();
    }
    String msg = record.getMessage();
    msg = msg != null ? msg : NULLMSG;
    if(record.getThrown() == null) {
      return msg.endsWith(OutputStreamLogger.NEWLINE) ? msg : (msg + OutputStreamLogger.NEWLINE);
    }
    // Enough space for some stack traces.
    StringBuilder buf = new StringBuilder(msg.length() + 500);
    buf.append(msg);
    if(!msg.endsWith(OutputStreamLogger.NEWLINE)) {
      buf.append(OutputStreamLogger.NEWLINE);
    }
    appendCauses(buf, record.getThrown());
    return buf.toString();
  }

  /**
   * Append (pruned) stack traces for associated exceptions.
   * 
   * @param buf Buffer to append to
   * @param thrown Throwable to format.
   */
  private void appendCauses(StringBuilder buf, Throwable thrown) {
    buf.append(thrown.toString()).append(OutputStreamLogger.NEWLINE);
    StackTraceElement[] stack = thrown.getStackTrace();
    int end = stack.length - 1;
    prune: for(; end >= 0; end--) {
      String cn = stack[end].getClassName();
      for(String pat : PRUNE) {
        if(cn.startsWith(pat)) {
          continue prune;
        }
      }
      break;
    }
    if(end <= 0) {
      end = stack.length - 1;
    }
    for(int i = 0; i <= end; i++) {
      buf.append("\tat ").append(stack[i]).append(OutputStreamLogger.NEWLINE);
    }
    if(end < stack.length - 1) {
      buf.append("\tat [...]").append(OutputStreamLogger.NEWLINE);
    }
    if(thrown.getCause() != null) {
      buf.append("Caused by: ");
      appendCauses(buf, thrown.getCause());
    }
  }
}
