package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Formatter;
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
import java.util.logging.LogRecord;

import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;

/**
 * Format a log record for error output, including a stack trace if available.
 * 
 * @author Erich Schubert
 */
// TODO: make more configurable and handle suppressed exceptions
public class ErrorFormatter extends Formatter {
  /**
   * List of packages to prune from stack traces at the end.
   * 
   * TODO: make configurable via logging.properties
   */
  public static String[] PRUNE = {//
  "de.lmu.ifi.dbs.elki.gui.minigui.MiniGUI", //
  "de.lmu.ifi.dbs.elki.KDDTask", //
  "java.awt.event.", //
  "java.awt.EventDispatchThread",//
  "java.awt.EventQueue",//
  "java.security",//
  "java.lang.Thread",//
  "java.util.concurrent.ThreadPoolExecutor",//
  "javax.swing.SwingWorker", //
  "java.util.concurrent.FutureTask", //
  "org.apache.batik.", //
  };

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
    StringBuffer buf = new StringBuffer(msg);
    if(!msg.endsWith(OutputStreamLogger.NEWLINE)) {
      buf.append(OutputStreamLogger.NEWLINE);
    }
    if(record.getThrown() != null) {
      appendCauses(buf, record.getThrown());
    }
    return buf.toString();
  }

  private void appendCauses(StringBuffer buf, Throwable thrown) {
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
      buf.append("Caused by: ").append(OutputStreamLogger.NEWLINE);
      appendCauses(buf, thrown.getCause());
    }
  }
}