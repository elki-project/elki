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
package de.lmu.ifi.dbs.elki.gui.util;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.lmu.ifi.dbs.elki.logging.ErrorFormatter;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.logging.MessageFormatter;
import de.lmu.ifi.dbs.elki.logging.OutputStreamLogger;
import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;

/**
 * A Swing object to receive ELKI logging output. Call
 * {@link #becomeDefaultLogger()} to register as default logger in ELKI.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @assoc - - - LoggingConfiguration
 * @assoc - - - de.lmu.ifi.dbs.elki.logging.ELKILogRecord
 * @assoc - - - ProgressLogRecord
 * @assoc - - - LogPaneHandler
 */
public class LogPane extends JTextPane {
  /**
   * Serialization version number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Base (default) style
   */
  protected Style baseStyle;

  /**
   * Regular message style
   */
  protected Style msgStyle;

  /**
   * Debug message style
   */
  protected Style dbgStyle;

  /**
   * Error message style
   */
  protected Style errStyle;

  /**
   * Formatter for regular messages (informational records)
   */
  private Formatter msgformat = new MessageFormatter();

  /**
   * Formatter for debugging messages
   */
  private Formatter debugformat = new ErrorFormatter();

  /**
   * Formatter for error messages
   */
  private Formatter errformat = new ErrorFormatter();

  /**
   * Last newline position
   */
  private int lastNewlinePos = 0;

  /**
   * Constructor
   */
  public LogPane() {
    super();
    // setup styles
    baseStyle = getStyledDocument().addStyle(null, null);
    msgStyle = getStyledDocument().addStyle("msg", baseStyle);
    errStyle = getStyledDocument().addStyle("err", baseStyle);
    errStyle.addAttribute(StyleConstants.Foreground, Color.RED);
    dbgStyle = getStyledDocument().addStyle("err", baseStyle);
    dbgStyle.addAttribute(StyleConstants.Foreground, Color.BLUE);
  }

  /**
   * Print a message as if it were logged, without going through the full
   * logger.
   * 
   * @param message
   *        Message text
   * @param level
   *        Message level
   */
  public void publish(String message, Level level) {
    try {
      publish(new LogRecord(level, message));
    }
    catch(BadLocationException e) {
      throw new RuntimeException("Error writing a log-like message.", e);
    }
  }

  /**
   * Publish a log record to the logging pane.
   * 
   * @param record
   *        Log record
   * @throws Exception
   */
  protected synchronized void publish(LogRecord record) throws BadLocationException {
    // choose an appropriate formatter
    final Formatter fmt;
    final Style style;
    // always format progress messages using the progress formatter.
    if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
      // format errors using the error formatter
      fmt = errformat;
      style = errStyle;
    }
    else if(record.getLevel().intValue() <= Level.FINE.intValue()) {
      // format debug statements using the debug formatter.
      fmt = debugformat;
      style = dbgStyle;
    }
    else {
      // default to the message formatter.
      fmt = msgformat;
      style = msgStyle;
    }
    // format
    final String m;
    m = fmt.format(record);
    StyledDocument doc = getStyledDocument();
    if(record instanceof ProgressLogRecord) {
      if(lastNewlinePos < doc.getLength()) {
        doc.remove(lastNewlinePos, doc.getLength() - lastNewlinePos);
      }
    }
    else {
      // insert a newline, if we didn't see one yet.
      if(lastNewlinePos < doc.getLength()) {
        doc.insertString(doc.getLength(), "\n", style);
        lastNewlinePos = doc.getLength();
      }
    }
    int tail = tailingNonNewline(m, 0, m.length());
    int headlen = m.length() - tail;
    if(headlen > 0) {
      String pre = m.substring(0, headlen);
      doc.insertString(doc.getLength(), pre, style);
    }
    lastNewlinePos = doc.getLength();
    if(tail > 0) {
      String post = m.substring(m.length() - tail);
      doc.insertString(lastNewlinePos, post, style);
    }
  }

  /**
   * Count the tailing non-newline characters.
   * 
   * @param str
   *        String
   * @param off
   *        Offset
   * @param len
   *        Range
   * @return number of tailing non-newline character
   */
  private int tailingNonNewline(String str, int off, int len) {
    for(int cnt = 0; cnt < len; cnt++) {
      final int pos = off + (len - 1) - cnt;
      if(str.charAt(pos) == OutputStreamLogger.UNIX_NEWLINE) {
        return cnt;
      }
      // TODO: need to compare to NEWLINE, too?
    }
    return len;
  }

  /**
   * Clear the current contents.
   */
  public void clear() {
    setText("");
    lastNewlinePos = 0;
  }

  /**
   * Become the default logger.
   */
  public void becomeDefaultLogger() {
    LoggingConfiguration.replaceDefaultHandler(new LogPaneHandler());
  }

  /**
   * Internal {@link java.util.logging.Handler}
   * 
   * @author Erich Schubert
   */
  private class LogPaneHandler extends Handler {
    /**
     * Constructor.
     */
    protected LogPaneHandler() {
      super();
    }

    @Override
    public void close() throws SecurityException {
      // do nothing
    }

    @Override
    public void flush() {
      // do nothing
    }

    @Override
    public void publish(LogRecord record) {
      if(EventQueue.isDispatchThread()) {
        try {
          LogPane.this.publish(record);
        }
        catch(Exception e) {
          reportError("Error printing output log message.", e, ErrorManager.GENERIC_FAILURE);
        }
      }
      else {
        SwingUtilities.invokeLater(() -> {
          try {
            LogPane.this.publish(record);
          }
          catch(Exception e) {
            reportError("Error printing output log message.", e, ErrorManager.GENERIC_FAILURE);
          }
        });
      }
    }
  }
}
