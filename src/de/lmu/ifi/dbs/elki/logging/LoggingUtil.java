package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This final class contains some static convenience methods for logging.
 * 
 * {@link #logExpensive} allows the programmer to easily emit a log message, however the function
 * is rather expensive and thus should not be used within loop constructs.
 * 
 * @author Erich Schubert
 */
public final class LoggingUtil {
  /**
   * Expensive logging function that is convenient, but should only be used in rare conditions.
   * 
   * For 'frequent' logging, use more efficient techniques, such as explained in the
   * {@link de.lmu.ifi.dbs.elki.logging logging package documentation}.
   * 
   * @param level Logging level
   * @param message Message to log.
   * @param e Exception to report.
   */
  public final static void logExpensive(Level level, String message, Throwable e) {
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
   * Expensive logging function that is convenient, but should only be used in rare conditions.
   * 
   * For 'frequent' logging, use more efficient techniques, such as explained in the
   * {@link de.lmu.ifi.dbs.elki.logging logging package documentation}.
   * 
   * @param level Logging level
   * @param message Message to log.
   */
  public final static void logExpensive(Level level, String message) {
    String[] caller = inferCaller();
    if(caller != null) {
      Logger logger = Logger.getLogger(caller[0]);
      logger.logp(level, caller[0], caller[1], message);
    }
    else {
      Logger.getAnonymousLogger().log(level, message);
    }
  }
  
  /**
   * Static version to log a severe exception.
   * 
   * @param message Exception message, may be null (defaults to e.getMessage())
   * @param e causing exception
   */
  public final static void exception(String message, Throwable e) {
    if (message == null && e != null) {
      message = e.getMessage();
    }
    logExpensive(LogLevel.SEVERE, message, e);
  }

  /**
   * Static version to log a warning message.
   * 
   * @param message Warning message.
   */
  public final static void warning(String message) {
    logExpensive(LogLevel.WARNING, message);    
  }

  /**
   * Static version to log a warning message.
   * 
   * @param message Warning message, may be null (defaults to e.getMessage())
   * @param e causing exception
   */
  public final static void warning(String message, Throwable e) {
    if (message == null && e != null) {
      message = e.getMessage();
    }
    logExpensive(LogLevel.WARNING, message, e);
  }

  /**
   * Infer which class has called the logging helper.
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
}
