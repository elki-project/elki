/**
 * 
 */
package experimentalcode.frankenb.log;

import java.io.PrintWriter;
import java.io.StringWriter;

import experimentalcode.frankenb.utils.Utils;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class TraceLevelLogFormatter implements ILogFormatter {

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.log.ILogFormatter#format(int, java.lang.StackTraceElement, experimentalcode.frankenb.log.LogLevel, java.lang.String)
   */
  @Override
  public String format(boolean mainThread, int methodDepth, long runTime, StackTraceElement callee, LogLevel level, String message, Throwable t) {
    String formattedMessage = String.format("%s[%s|%s.%s()|%5s]  %s", repeat("\t", methodDepth), Utils.formatRunTime(runTime), simpleClassNameOf(callee.getClassName()), callee.getMethodName(), level.toString(), message);
    if (t == null) {
      return formattedMessage;
    } else {
      StringWriter out = new StringWriter();
      PrintWriter s = new PrintWriter(out);
      s.append(formattedMessage);
      s.append('\n');
      t.printStackTrace(s);
      s.flush();
      return out.toString();
    }
  }
  
  private static String simpleClassNameOf(String className) {
    return className.substring(className.lastIndexOf(".") + 1);
  }
  
  private static String repeat(String str, int times) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < times; ++i) {
      sb.append(str);
    }
    return sb.toString();
  }  

}
