/**
 * 
 */
package experimentalcode.frankenb.log;

import java.io.PrintWriter;
import java.io.StringWriter;

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
    String formattedMessage = String.format("%s[%s|%s.%s()|%5s]  %s", repeat("\t", methodDepth), formatRunTime(runTime), simpleClassNameOf(callee.getClassName()), callee.getMethodName(), level.toString(), message);
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
  
  private static long[] timeAmounts = new long[] {1L, 1000L, 60000L, 3600000L, 86400000L};
  private static String[] timeNames = new String[] {"ms", "s", "m", "h", "d"};
  private static int[] timeDigits = new int[] {3, 2, 2, 2, 2};
  
  private static String formatRunTime(long time) {
    StringBuilder sb = new StringBuilder();
    
    for (int i = timeAmounts.length - 1; i >= 0; --i) {

      //if we have minutes already then we can ignore the ms
      if (i == 0 && sb.length() > 4) continue;
      if (sb.length() > 0) {
        sb.append(" ");
      }
      
      long acValue = time / timeAmounts[i];
      time = time % timeAmounts[i];
      if (!(acValue == 0 && sb.length() == 0)) {
        sb.append(String.format("%0" + timeDigits[i] + "d%s", acValue, timeNames[i]));
      }
    }
    return sb.toString();
  }

}
