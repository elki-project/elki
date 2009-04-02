package de.lmu.ifi.dbs.elki.logging;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.utilities.Progress;

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
   * @param c Class name to retrieve logging for
   * @return Logger
   */
  public static Logging getLogger(final String name) {
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
   * @return
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
    return logger.isLoggable(LogLevel.INFO);
  }

  /**
   * Test whether to log 'debug' at 'FINE' level
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebugging() {
    return logger.isLoggable(LogLevel.FINE);
  }

  /**
   * Test whether to log 'debug' at 'FINER' level
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebuggingFiner() {
    return logger.isLoggable(LogLevel.FINER);
  }

  /**
   * Test whether to log 'debug' at 'FINEST' level
   * 
   * @return true if debug logging enabled
   */
  public boolean isDebuggingFinest() {
    return logger.isLoggable(LogLevel.FINEST);
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
   * Log a message at the 'warning' level.
   * 
   * @param message Warning log message.
   * @param e Exception
   */
  public void warning(String message, Throwable e) {
    log(LogLevel.WARNING, message, e);
  }

  /**
   * Log a message at the 'warning' level.
   * 
   * @param message Warning log message.
   */
  public void warning(String message) {
    log(LogLevel.WARNING, message);
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
    log(LogLevel.INFO, message, e);
  }

  /**
   * Log a message at the 'info' ('verbose') level.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param message Informational log message.
   */
  public void verbose(String message) {
    log(LogLevel.INFO, message);
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
    log(LogLevel.FINE, message, e);
  }

  /**
   * Log a message at the 'fine' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFine(String message) {
    log(LogLevel.FINE, message);
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
    log(LogLevel.FINER, message, e);
  }

  /**
   * Log a message at the 'finer' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFiner(String message) {
    log(LogLevel.FINER, message);
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
    log(LogLevel.FINEST, message, e);
  }

  /**
   * Log a message at the 'finest' debugging level.
   * 
   * You should check isDebugging() before building the message.
   * 
   * @param message Informational log message.
   */
  public void debugFinest(String message) {
    log(LogLevel.FINEST, message);
  }

  /**
   * Log a message with exception at the 'severe' level.
   * 
   * @param message Error log message.
   * @param e Exception
   */
  public void exception(String message, Throwable e) {
    log(LogLevel.SEVERE, message, e);
  }

  /**
   * Log a message at the 'progress' level.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param message Informational log message.
   * @param e Exception
   */
  public void progress(String message, Throwable e) {
    log(LogLevel.PROGRESS, message, e);
  }

  /**
   * Log a message at the 'progress' level.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param message Informational log message.
   */
  public void progress(String message) {
    log(LogLevel.PROGRESS, message);
  }

  /**
   * Log a 'progress' log record.
   * 
   * You should check isVerbose() before building the message.
   * 
   * @param rec Log record.
   */
  public void progress(LogRecord rec) {
    logger.log(rec);
  }
  
  /**
   * Log a Progress class. 
   * 
   * @param pgr Progress to log.
   */
  public void progress(Progress pgr) {
    logger.log(new ProgressLogRecord("\r" + pgr.toString(), pgr.getTask(), pgr.status()));
  }
}
