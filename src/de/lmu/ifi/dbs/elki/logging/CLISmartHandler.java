package de.lmu.ifi.dbs.elki.logging;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import de.lmu.ifi.dbs.elki.logging.progress.Progress;
import de.lmu.ifi.dbs.elki.logging.progress.ProgressLogRecord;
import de.lmu.ifi.dbs.elki.logging.progress.ProgressTracker;

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
   * Tracker for progress messages
   */
  private ProgressTracker ptrack = new ProgressTracker();

  /**
   * Constructor
   * 
   * @param out Regular output stream
   * @param err Error output stream
   */
  public CLISmartHandler(final OutputStream out, final OutputStream err) {
    super();
    this.out = new OutputStreamLogger(out);
    this.err = new OutputStreamLogger(err);
  }

  /**
   * Default constructor using {@link System#out} and {@link System#err}
   */
  public CLISmartHandler() {
    this(System.out, System.err);
  }

  /**
   * Close output streams.
   */
  @Override
  public void close() throws SecurityException {
    // we have wrapped the output streams in OutputStreamLogger anyway
    // which will ignore any close() call.
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
    if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
      destination = this.err;
    }
    else {
      destination = this.out;
    }

    // format
    final String m;

    // Progress records are handled specially.
    if(record instanceof ProgressLogRecord) {
      ProgressLogRecord prec = (ProgressLogRecord) record;
      ptrack.addProgress(prec.getProgress());
      // TODO: rate control?

      Collection<Progress> completed = ptrack.removeCompleted();
      Collection<Progress> progresses = ptrack.getProgresses();

      StringBuffer buf = new StringBuffer();
      if(completed.size() > 0) {
        buf.append(OutputStreamLogger.CARRIAGE_RETURN);
        for(Progress prog : completed) {
          // TODO: use formatter, somehow?
          prog.appendToBuffer(buf);
          buf.append(OutputStreamLogger.NEWLINE);
        }
      }
      if(progresses.size() > 0) {
        boolean first = true;
        buf.append(OutputStreamLogger.CARRIAGE_RETURN);
        for(Progress prog : progresses) {
          if(first) {
            first = false;
          }
          else {
            buf.append(" ");
          }
          // TODO: use formatter, somehow?
          prog.appendToBuffer(buf);
        }
      }
      m = buf.toString();
    }
    else {
      // choose an appropriate formatter
      final Formatter fmt;
      // always format progress messages using the progress formatter.
      if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
        // format errors using the error formatter
        fmt = errformat;
      }
      else if(record.getLevel().intValue() <= Level.FINE.intValue()) {
        // format debug statements using the debug formatter.
        fmt = debugformat;
      }
      else {
        // default to the message formatter.
        fmt = msgformat;
      }
      try {
        m = fmt.format(record);
      }
      catch(Exception ex) {
        reportError(null, ex, ErrorManager.FORMAT_FAILURE);
        return;
      }
    }
    // write
    try {
      destination.write(m);
      // always flush (although the streams should auto-flush already)
      destination.flush();
    }
    catch(Exception ex) {
      reportError(null, ex, ErrorManager.WRITE_FAILURE);
      return;
    }
  }
}
