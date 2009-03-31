package de.lmu.ifi.dbs.elki.logging;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Handler that handles output to the console with clever formatting.
 * 
 * @author Erich Schubert
 */
public class CLISmartHandler extends Handler {
  /**
   * Output stream for non-critical output.
   */
  private Writer out;

  /**
   * Output stream for error output.
   */
  private Writer err;

  /**
   * Formatter for Progress records
   */
  private Formatter progformat = new ProgressFormatter();

  /**
   * Formatter for regular messages (informational records)
   */
  private Formatter msgformat = new MessageFormatter();

  /**
   * Formatter for debugging messages
   */
  private Formatter debugformat = new SimpleFormatter();

  /**
   * Formatter for error messages
   */
  private Formatter errformat = new SimpleFormatter();

  /**
   * Constructor
   * 
   * @param out Regular output stream
   * @param err Error output stream
   */
  public CLISmartHandler(final OutputStream out, final OutputStream err) {
    super();
    this.out = new OutputStreamWriter(out);
    this.err = new OutputStreamWriter(err);
  }

  /**
   * Default constructor using {@link System.out} and {@link System.err}
   */
  public CLISmartHandler() {
    this(new NonClosingOutputStream(System.out), new NonClosingOutputStream(System.err));
  }

  /**
   * Close output streams.
   */
  @Override
  public void close() throws SecurityException {
    try {
      out.close();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.CLOSE_FAILURE);
    }
    try {
      err.close();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.CLOSE_FAILURE);
    }
  }

  /**
   * Flush output streams
   */
  @Override
  public void flush() {
    try {
      out.flush();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.FLUSH_FAILURE);
    }
    try {
      err.flush();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.FLUSH_FAILURE);
    }
  }

  /**
   * Publish a log record.
   */
  @Override
  public void publish(final LogRecord record) {
    // determine destination
    final Writer destination;
    if(record.getLevel().intValue() >= LogLevel.WARNING.intValue()) {
      destination = this.err;
    }
    else {
      destination = this.out;
    }
    // choose an appropriate formatter
    final Formatter fmt;
    // always format progress messages using the progress formatter.
    if(record instanceof ProgressLogRecord) {
      fmt = progformat;
    }
    else if(record.getLevel().intValue() >= LogLevel.WARNING.intValue()) {
      // format errors using the error formatter
      fmt = errformat;
    }
    else if(record.getLevel().intValue() <= LogLevel.FINE.intValue()) {
      // format debug statements using the debug formatter.
      fmt = debugformat;
    }
    else {
      // default to the message formatter.
      fmt = msgformat;
    }
    // format
    final String m;
    try {
      m = fmt.format(record);
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.FORMAT_FAILURE);
      return;
    }
    // write
    try {
      destination.write(m);
      // always flush.
      destination.flush();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.WRITE_FAILURE);
      return;
    }
  }
}
