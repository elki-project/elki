/**
 * 
 */
package experimentalcode.frankenb.log;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface ILogFormatter {

  public String format(boolean mainThread, int methodDepth, long runTime, StackTraceElement callee, LogLevel level, String message, Throwable t);
  
}
