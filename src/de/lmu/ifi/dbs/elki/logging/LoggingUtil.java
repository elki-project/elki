package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class with utilities used in logging.
 * 
 * @author Erich Schubert
 */
public final class LoggingUtil {
  /**
   * Expensive logging function that is convenient, but should only be used in rare conditions.
   * 
   * For 'frequent' logging, use more efficient techniques!
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
   * For 'frequent' logging, use more efficient techniques!
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
