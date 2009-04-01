package de.lmu.ifi.dbs.elki.logging;

import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Abstract superclass for classes being loggable, i.e. classes intending to log
 * messages.
 * <p/>
 * 
 * @author Steffi Wanka
 */
public abstract class AbstractLoggable {
  static {
    LoggingConfiguration.assertConfigured();
  }

  /**
   * Holds the class specific debug status.
   */
  protected boolean debug;

  /**
   * The logger of the class.
   */
  protected final Logger logger;

  /**
   * Initializes the logger and sets the debug status to the given value.
   * 
   * @param debug the debug status.
   */
  protected AbstractLoggable(boolean debug) {
    this.logger = Logger.getLogger(this.getClass().getName());
    this.debug = debug;
  }

  /**
   * Initializes the logger with the given name and sets the debug status to the
   * given value.
   * 
   * @param debug the debug status.
   * @param name the name of the logger.
   */
  protected AbstractLoggable(boolean debug, String name) {
    this.logger = Logger.getLogger(name);
    this.debug = debug;
  }

  /**
   * Log an exception at SEVERE level.
   * <p/>
   * If the logger is currently enabled for the SEVERE message level then the
   * given message is forwarded to all the registered output Handler objects.
   * 
   * Depreciated:
   * 
   * use {@link LoggingUtil.logExpensive(LogLevel.SEVERE, msg, e) instead.
   */
  public void exception(String msg, Throwable e) {
    logger.log(LogLevel.SEVERE, msg, e);
  }

  /**
   * Log a WARNING message.
   * <p/>
   * If the logger is currently enabled for the WARNING message level then the
   * given message is forwarded to all the registered output Handler objects.
   * 
   * Depreciated:
   * 
   * use {@link LoggingUtil.logExpensive(LogLevel.WARNING, msg) instead.
   */
  public void warning(String msg) {
    logger.log(LogLevel.WARNING, msg);
  }

  /**
   * Log a message with Level INFO.
   * <p/>
   * If the logger is currently enabled for the INFO level then the given
   * message is forwarded to all the registered output Handler objects.
   * 
   * Depreciated:
   * 
   * Use
   * 
   * <pre>
   * if (logger.isLoggable(LogLevel.INFO)) {
   *   logger.log(LogLevel.INFO, msg);
   * }
   * </pre>
   * 
   * instead. If msg is a constant, you can leave away the if statement.
   */
  public void info(String msg) {
    logger.log(LogLevel.INFO, msg);
  }

  /**
   * Log a PROGRESS message.
   * <p/>
   * If the logger is currently enabled for the PROGRESS message level then the
   * given message is forwarded to all the registered output Handler objects.
   */
  public void progress(Progress pgr) {
    logger.log(new ProgressLogRecord(Util.status(pgr), pgr.getTask(), pgr.status()));
  }

  /**
   * Log a PROGRESS message.
   * <p/>
   * If the logger is currently enabled for the PROGRESS message level then the
   * given message is forwarded to all the registered output Handler objects.
   * 
   * @param pgr the progress to be logged
   * @param numClusters The current number of clusters
   * @see Loggable#progress(de.lmu.ifi.dbs.elki.utilities.Progress)
   */
  public void progress(Progress pgr, int numClusters) {
    logger.log(new ProgressLogRecord(Util.status(pgr, numClusters), pgr.getTask(), pgr.status()));
  }

  /**
   * Log a PROGRESS message.
   * <p/>
   * If the logger is currently enabled for the PROGRESS message level then the
   * given message is forwarded to all the registered output Handler objects.
   */
  public void progress(LogRecord record) {
    logger.log(record);
  }

  /**
   * Log a VERBOSE message.
   * <p/>
   * If the logger is currently enabled for the VERBOSE message level then the
   * given message is forwarded to all the registered output Handler objects.
   */
  public void verbose(String msg) {
    logger.log(LogLevel.VERBOSE, msg + System.getProperty("line.separator"));
  }

  /**
   * Log an empty VERBOSE message.
   * <p/>
   * If the logger is currently enabled for the VERBOSE message level then the
   * given message is forwarded to all the registered output Handler objects.
   * <p/>
   * Use this method to insert a newline in the verbose log.
   * 
   * @see #verbose(String)
   */
  public void verbose() {
    verbose("");
  }

  /**
   * Log a DEBUG_FINE message.
   * <p/>
   * If the logger is currently enabled for the DEBUG_FINE message level then
   * the given message is forwarded to all the registered output Handler
   * objects.
   * 
   * Depreciated:
   * 
   * Use
   * 
   * <pre>
   * if (logger.isLoggable(LogLevel.FINE)) {
   *   logger.log(LogLevel.FINE, msg);
   * }
   * </pre>
   * 
   * instead. If msg is a constant, you can leave away the if statement.
   */
  public void debugFine(String msg) {
    LogRecord record = new LogRecord(LogLevel.FINE, msg);
    record.setSourceClassName(this.getClass().getName());
    record.setSourceMethodName(inferCaller(this.getClass().getName()));
    logger.log(record);
  }

  /**
   * Log a DEBUG_FINER message.
   * <p/>
   * If the logger is currently enabled for the DEBUG_FINER message level then
   * the given message is forwarded to all the registered output Handler
   * objects.
   * 
   * Depreciated:
   * 
   * Use
   * 
   * <pre>
   * if (logger.isLoggable(LogLevel.FINER)) {
   *   logger.log(LogLevel.FINER, msg);
   * }
   * </pre>
   * 
   * instead. If msg is a constant, you can leave away the if statement.
   */
  public void debugFiner(String msg) {
    LogRecord record = new LogRecord(LogLevel.FINER, msg);
    record.setSourceClassName(this.getClass().getName());
    record.setSourceMethodName(inferCaller(this.getClass().getName()));
    logger.log(record);
  }

  /**
   * Log a DEBUG_FINEST message.
   * <p/>
   * If the logger is currently enabled for the DEBUG_FINEST message level then
   * the given message is forwarded to all the registered output Handler
   * objects.
   * 
   * Depreciated:
   * 
   * Use
   * 
   * <pre>
   * if (logger.isLoggable(LogLevel.FINEST)) {
   *   logger.log(LogLevel.FINEST, msg);
   * }
   * </pre>
   * 
   * instead. If msg is a constant, you can leave away the if statement.
   */
  public void debugFinest(String msg) {
    LogRecord record = new LogRecord(LogLevel.FINEST, msg);
    record.setSourceClassName(this.getClass().getName());
    record.setSourceMethodName(inferCaller(this.getClass().getName()));
    logger.log(record);
  }

  // Private method to infer the caller's class and method names
  // TODO comment
  private String inferCaller(String className) {

    String methodName = null;
    StackTraceElement stack[] = (new Throwable()).getStackTrace();
    int ix = 0;
    while(ix < stack.length) {
      StackTraceElement frame = stack[ix];

      if(frame.getClassName().equals(className)) {
        return frame.getMethodName();
      }
      ix++;
    }

    return methodName;
  }
}
