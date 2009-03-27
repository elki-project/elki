package de.lmu.ifi.dbs.elki.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Facility for configuration of logging.
 * 
 * @author Arthur Zimek
 */
public final class LoggingConfiguration {
  /**
   * Whether the LoggingConfiguration is still changeable.
   */
  private static boolean configurationChangeable = true;

  /**
   * General debug flag.
   */
  public static boolean DEBUG = false;

  /**
   * General stack-trace flag.
   */
  public static boolean STACKTRACE = false;

  /**
   * Configuration code for command line interface.
   */
  public static final int CLI = 0;

  /**
   * Configuration file name
   */
  private static final String CLIConffile = "logging-cli.properties";

  /**
   * Configuration base
   */
  private static final String confbase = LoggingConfiguration.class.getPackage().getName();

  /**
   * Static instance of the configuration
   */
  protected static LoggingConfiguration config = new LoggingConfiguration();

  /**
   * Configure Java Logging API: {@link java.util.logging}
   */
  private LoggingConfiguration() {
    LogManager logManager = LogManager.getLogManager();
    Logger logger = Logger.getLogger(LoggingConfiguration.class.getName());
    // Ensure that our log levels are loaded before parsing the configuration
    // file.
    LogLevel.VERBOSE.getName();

    // Load logging configuration from resources.
    try {
      InputStream cfgdata = null;
      // try in the file system first
      File cfgfile = new File(confbase.replace('.', File.separatorChar) + File.separatorChar + CLIConffile);
      if(cfgfile.exists() && cfgfile.canRead()) {
        cfgdata = new FileInputStream(cfgfile.getAbsolutePath());
      }
      else // otherwise, load from system resources
      {
        cfgdata = ClassLoader.getSystemResourceAsStream(confbase.replace('.', '/') + '/' + CLIConffile);
      }
      // Load logging configuration
      if(cfgdata != null) {
        logManager.readConfiguration(cfgdata);
        logger.info("Logging configuration read.");
      }
      else {
        logger.warning("No logging configuration found.");
      }
    }
    catch(Exception e) {
      logger.log(Level.SEVERE, "Failed to configure logging.", e);
    }
    // extra config settings
    // try in the file system first
    File cfgfile = new File(confbase.replace('.', File.separatorChar) + File.separatorChar + CLIConffile);
    Properties cfgprop = new Properties();
    try {
      cfgprop.load(ClassLoader.getSystemResourceAsStream(confbase.replace('.', '/') + '/' + CLIConffile));
      if(cfgfile.exists() && cfgfile.canRead()) {
        cfgprop.load(new FileInputStream(cfgfile.getAbsolutePath()));
      }
    }
    catch(Exception e) {
      logger.log(Level.SEVERE, "Failed to load logging properties.", e);
    }
    DEBUG = Boolean.valueOf(cfgprop.getProperty("debug"));
    STACKTRACE = Boolean.valueOf(cfgprop.getProperty("stack-trace"));
  }

  /**
   * Configures the specified logger according to the specified configuration
   * code.
   * 
   * @param logger the logger to configure
   * @param configuration the configuration code
   */
  public void configure(Logger logger, int configuration) {
    switch(configuration){
    case CLI:
      replaceHandlers(logger, consoleHandlers());
      break;
    default:
      throw new IllegalArgumentException("unknown configuration code " + configuration);
    }
  }

  /**
   * Configures the given logger. Removes all handlers currently associated with
   * the logger and associates the given handlers instead.
   * 
   * @param logger the logger to configure
   * @param handler the handlers to associate with the logger
   */
  public void replaceHandlers(Logger logger, Handler[] handler) {
    Handler[] oldHandler = logger.getHandlers();
    for(Handler h : oldHandler) {
      logger.removeHandler(h);
    }
    for(Handler h : handler) {
      logger.addHandler(h);
    }
  }

  /**
   * Provides the standard handlers for command line interface configuration.
   * <ul>
   * <li>Debugging: Debug messages are printed immediately to
   * <code>System.err</code>.</li>
   * <li>Verbose messages for regular user information are printed immediately
   * to <code>System.out</code>.</li>
   * <li>Warning messages for user information are printed immediately to
   * <code>System.err</code>.</li>
   * <li>Exception messages are printed immediately to <code>System.err</code>.</li>
   * </ul>
   * 
   * @return an array of four CLI handlers
   */
  protected Handler[] consoleHandlers() {
    // return new Handler[] { new ConsoleHandler() };
    // TODO: perhaps more suitable formatters?
    Handler exceptionHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.err), new ExceptionFormatter(STACKTRACE), new SelectiveFilter(LogLevel.EXCEPTION));
    Handler warningHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.err), new SimpleFormatter(), new SelectiveFilter(LogLevel.WARNING));
    Handler messageHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.out), new MessageFormatter(), new SelectiveFilter(LogLevel.MESSAGE));
    Handler progressHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.out), new ProgressFormatter(), new SelectiveFilter(LogLevel.PROGRESS));
    Handler verboseHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.out), new MessageFormatter(), new SelectiveFilter(LogLevel.VERBOSE));
    Handler debugHandler = new ImmediateFlushHandler(new NonClosingOutputStream(System.err), new SimpleFormatter(), new SelectiveFilter(LogLevel.FINE));
    Handler[] consoleHandlers = { debugHandler, verboseHandler, warningHandler, exceptionHandler, messageHandler, progressHandler };
    return consoleHandlers;
  }

  /**
   * Configures the root logger according to the specified configuration code.
   * <p/>
   * The configuration will only be set, if {@link #configurationChangeable
   * configurationChangeable} is true. After this method has been called, the
   * logging configuration cannot be changed again by a method of this class.
   * 
   * @param configuration the configuration code
   */
  public static void configureRootFinally(int configuration) {
    if(configurationChangeable) {
      LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
      loggingConfiguration.configure(Logger.getLogger(""), configuration);
    }
    else {
      Logger.getLogger(LoggingConfiguration.class.getName()).warning("logger configuration cannot be changed\n");
    }
    configurationChangeable = false;
  }

  /**
   * Configures the root logger according to the specified configuration code.
   * <p/>
   * The configuration will only be set, if {@link #configurationChangeable
   * configurationChangeable} is true.
   * 
   * @param configuration the configuration code
   */
  public static void configureRoot(int configuration) {
    if(configurationChangeable) {
      LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
      loggingConfiguration.configure(Logger.getLogger(""), configuration);
    }
    else {
      Logger.getLogger(LoggingConfiguration.class.getName()).warning("logger configuration cannot be changed");
    }
  }

  /**
   * Returns whether the LoggingConfiguration is still changeable.
   * 
   * @return a boolean indicating whether the LoggingConfiguration is still
   *         changeable
   */
  public static boolean isChangeable() {
    return configurationChangeable;
  }
}
