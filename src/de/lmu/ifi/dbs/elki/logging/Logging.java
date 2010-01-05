package de.lmu.ifi.dbs.elki.logging;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.logging.progress.Progress;

/**
 * This class is a wrapper around {@link java.util.logging.Logger} and
 * {@link java.util.logging.LogManager} offering additional convenience functions.
 * 
 * If a class keeps a static reference to the appropriate {@link Logging} object,
 * performance penalty compared to standard logging should be minimal.
 * 
 * However when using {@link java.util.logging.LogRecord} directly instead of 
 * {@link ElkiLogRecord}, the use of the {@link #log(LogRecord)} method will result in
 * incorrectly logged cause location. Therefore, use {@link ElkiLogRecord}!
 * 
 * @author Erich Schubert
 *
 */
public class Logging {
  /**
   * HashMap to keep track of loggers.
   */
  private static HashMap<String, Logging> loggers = new HashMap<String, Logging>();

  /**
   * Wrapped logger
   */
  private final Logger logger;

  /**
   * Constructor, wrapping a logger.
   * 
   * @param logger Logger to wrap.
   */
  public Logging(final Logger logger) {
    this.logger = logger;
  }

  /**
   * Retrieve logging utility for a particular class.
   * 
   * @param c Class to retrieve logging for
   * @return Logger
   */
  public static Logging getLogger(final Class<?> c) {
    return getLogger(c.getName());
  }

  /**
   * Retrieve logging utility for a particular class.
   * 
   * @param name Class name 
   * @return Logger
   */
  public synchronized static Logging getLogger(final String name) {
    Logging logger = loggers.get(name);
    if(logger == null) {
      logger = new Logging(Logger.getLogger(name));
      loggers.put(name, logger);
    }
    return logger;
  }

  /**
   * Verify if logging is enabled at that particular level.
   * 
   * @param lev Logging level
   * @return status
   */
  public boolean isLoggable(Level lev) {
    return logger.isLoggable(lev);
  }
  
  /**
   * Test whether to log 'verbose'.
   * 
   * @return true if verbose
   */
  public boolean isVerbose() {
    return logger.isLoggable(Level.INFO);
  }

  /**
   * Test whether to log 'debug' at 'FINE' level.
   * 
   * This is the same as {@link #isDebuggingFine}
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebugging() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   * Test whether to log 'debug' at 'FINE' level
   * 
   * This is the same as {@link #isDebugging}
   *  
   * @return true if debug logging enabled
   */
  public boolean isDebuggingFine() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   * Test whether to log 'debug' at 'FINER' level
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebuggingFiner() {
    return logger.isLoggable(Level.FINER);
  }

  /**
   * Test whether to log 'debug' at 'FINEST' level
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebuggingFinest() {
    return logger.isLoggable(Level.FINEST);
  }

  /**
   * Log a log message at the given level.
   * 
   * @param level Level to log at.
   * @param message Message to log.
   */
  public void log(Level level, String message) {
    LogRecord rec = new ElkiLogRecord(level, message);
    logger.log(rec);
  }

  /**
   * Log a log message and exception at the given level.
   * 
   * @param level Level to log at.
   * @param message Message to log.
   * @param e Exception
   */
  public void log(Level level, String message, Throwable e) {
    LogRecord rec = new ElkiLogRecord(level, message);
    rec.setThrown(e);
    logger.log(rec);
  }

  /**
   * Log a given log record (should be a {@link ElkiLogRecord})
   * 
   * @param rec Log record to log.
   */
  public void log(LogRecord rec) {
    logger.log(rec);
  }

  /**
   * Log a message at the 'severe' level.
   * 
   * @param message Warning log message.
   * @param e Exception
   */
  public void error(String message, Throwable e) {
    log(Level.SEVERE, message, e);
  }

  /**
   * Log a message at the 'severe' level.
   * 
   * @param message Warning log message.
   */
  public void error(String message) {
    log(Level.SEVERE, message);
  }

  /**
   * Log a message at the 'warning' level.
   * 
   * @param message Warning log message.
   * @param e Exception
   */
  public void warning(String message, Throwable e) {
    log(Level.WARNING, message, e);
  }

  /**
   * Log a message at the 'warning' level.
   * 
   * @param message Warning log message.
   */
  public void warning(String message) {
    log(Level.WARNING, message);
  }

  /**
   * Log a message at the 'info' ('verbose') level.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void verbose(String message, Throwable e) {
    log(Level.INFO, message, e);
  }

  /**
   * Log a message at the 'info' ('verbose') level.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param message Informational log message.
   */
  public void verbose(String message) {
    log(Level.INFO, message);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void debug(String message, Throwable e) {
    log(Level.FINE, message, e);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debug(String message) {
    log(Level.FINE, message);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void debugFine(String message, Throwable e) {
    log(Level.FINE, message, e);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFine(String message) {
    log(Level.FINE, message);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void fine(String message, Throwable e) {
    log(Level.FINE, message, e);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void fine(String message) {
    log(Level.FINE, message);
  }

  /**
   * Log a message at the 'finer' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void debugFiner(String message, Throwable e) {
    log(Level.FINER, message, e);
  }

  /**
   * Log a message at the 'finer' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFiner(String message) {
    log(Level.FINER, message);
  }

  /**
   * Log a message at the 'finer' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void finer(String message, Throwable e) {
    log(Level.FINER, message, e);
  }

  /**
   * Log a message at the 'finer' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void finer(String message) {
    log(Level.FINER, message);
  }

  /**
   * Log a message at the 'finest' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void debugFinest(String message, Throwable e) {
    log(Level.FINEST, message, e);
  }

  /**
   * Log a message at the 'finest' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFinest(String message) {
    log(Level.FINEST, message);
  }

  /**
   * Log a message at the 'finest' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void finest(String message, Throwable e) {
    log(Level.FINEST, message, e);
  }

  /**
   * Log a message at the 'finest' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void finest(String message) {
    log(Level.FINEST, message);
  }

  /**
   * Log a message with exception at the 'severe' level.
   * 
   * @param message Error log message.
   * @param e Exception
   */
  public void exception(String message, Throwable e) {
    log(Level.SEVERE, message, e);
  }

  /**
   * Log an exception at the 'severe' level.
   * 
   * @param e Exception
   */
  public void exception(Throwable e) {
    log(Level.SEVERE, e.getMessage(), e);
  }

  /**
   * Log a Progress object. 
   * 
   * @param pgr Progress to log.
   */
  public void progress(Progress pgr) {
    StringBuffer buf = new StringBuffer();
    buf.append(OutputStreamLogger.CARRIAGE_RETURN);
    pgr.appendToBuffer(buf);
    logger.log(Level.INFO, buf.toString());
  }

  /**
   * Log a Progress object. 
   * 
   * @param pgr1 First progress to log.
   * @param pgr2 Second progress to log.
   */
  public void progress(Progress pgr1, Progress pgr2) {
    StringBuffer buf = new StringBuffer();
    buf.append(OutputStreamLogger.CARRIAGE_RETURN);
    pgr1.appendToBuffer(buf);
    buf.append(" ");
    pgr2.appendToBuffer(buf);
    logger.log(Level.INFO, buf.toString());
  }

  /**
   * @return the wrapped {@link java.util.logging.Logger}
   */
  public Logger getWrappedLogger() {
    return logger;
  }
}
