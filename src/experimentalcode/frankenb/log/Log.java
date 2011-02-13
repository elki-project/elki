/**
 * 
 */
package experimentalcode.frankenb.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class Log {

  private static ILogFormatter logFormatter = null;
  private static List<Pair<LogLevel, ILogWriter>> logWriters = new ArrayList<Pair<LogLevel, ILogWriter>>();

  private static Map<String, Integer> defaultDepths = new HashMap<String, Integer>();
  
  private static long startTime = System.currentTimeMillis();
  private static LogLevel filterLogLevel = LogLevel.DEBUG;
  
  private static boolean showedWarning = false;
  
  private Log() {}
  
  public static void setFilter(LogLevel logLevel) {
    filterLogLevel = logLevel;
  }
  
  public static void verbose() {
    verbose("");
  }
  
  public static void verbose(String message) {
    verbose(message, null);
  }
  
  public static void verbose(String message, Throwable t) {
    log(LogLevel.VERBOSE, message, t);
  }
  
  public static void debug() {
    debug("");
  }
  
  public static void debug(String message) {
    debug(message, null);
  }
  
  public static void debug(String message, Throwable t) {
    log(LogLevel.DEBUG, message, t);
  }
  
  public static void info() {
    info("");
  }
  
  public static void info(String message) {
    info(message, null);
  }
  
  public static void info(String message, Throwable t) {
    log(LogLevel.INFO, message, t);
  }
  
  public static void warn() {
    warn("");
  }
  
  public static void warn(String message) {
    warn(message, null);
  }
  
  public static void warn(String message, Throwable t) {
    log(LogLevel.WARN, message, t);
  }
  
  public static void error() {
    error("");
  }
  
  public static void error(String message) {
    error(message, null);
  }
  
  public static void error(String message, Throwable t) {
    log(LogLevel.ERROR, message, t);
  }
  
  
  public static void log(LogLevel level, String message, Throwable t) {
    if (logFormatter == null) {
      if (!showedWarning) {
        System.out.println("No logFormatter specified!");
        showedWarning = true;
      }
      return;
    }
    if (level.ordinal() < filterLogLevel.ordinal()) return;
    
    List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>(Arrays.asList(Thread.currentThread().getStackTrace()));
    stackTrace.remove(0);
    
    while (stackTrace.get(0).getClassName().equals(Log.class.getName())) {
      stackTrace.remove(0);
    }
    
    StackTraceElement callee = stackTrace.get(0); 
    
    StackTraceElement thread = stackTrace.get(stackTrace.size() - 1);
    boolean isMainThread = thread.getMethodName().equals("main");
    String threadClass = thread.getClassName();
    
    int depth = stackTrace.size();
     
    if (!defaultDepths.containsKey(threadClass)) {
      defaultDepths.put(threadClass, depth);
    }
    int defaultDepth = defaultDepths.get(threadClass);
    
    String formattedMessage = logFormatter.format(isMainThread, depth - defaultDepth, getElapsedTime(), callee, level, message, t);
    
    for (Pair<LogLevel, ILogWriter> pair : logWriters) {
      ILogWriter logWriter = pair.getSecond();
      if (level.ordinal() >= pair.getFirst().ordinal()) {
        logWriter.putLogLine(level, formattedMessage);
      }
    }
  }
  
  public static void setLogFormatter(ILogFormatter logFormatter) {
    Log.logFormatter = logFormatter;
  }
  
  public static void addLogWriter(ILogWriter logWriter) {
    addLogWriter(logWriter, LogLevel.VERBOSE);
  }
  
  public static void addLogWriter(ILogWriter logWriter, LogLevel filterLevel) {
    Log.logWriters.add(new Pair<LogLevel, ILogWriter>(filterLevel, logWriter));
  }
  
  public static void removeLogWriter(ILogWriter logWriter) {
    int positionToRemove = -1;
    for (int i = 0; i < logWriters.size(); ++i) {
      ILogWriter aLogWriter = logWriters.get(i).getSecond();
      if (aLogWriter.equals(logWriter)) {
        positionToRemove = i;
      }
    }
    if (positionToRemove != -1) {
      logWriters.remove(positionToRemove);
    }
  }

  public static long getElapsedTime() {
    return System.currentTimeMillis() - startTime;
  }
  
}
