package de.lmu.ifi.dbs.elki.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.utilities.FileUtil;

/**
 * Facility for configuration of logging.
 * 
 * @author Erich Schubert
 */
public final class LoggingConfiguration {
  /**
   * General debug flag.
   */
  public static boolean DEBUG = false;

  /**
   * Configuration file name
   */
  private static final String CLIConffile = "logging-cli.properties";

  /**
   * Top level ELKI package (for setting 'verbose')
   */
  private static final String TOPLEVEL_PACKAGE = "de.lmu.ifi.dbs.elki";

  /**
   * Configuration base
   */
  private static final String confbase = LoggingConfiguration.class.getPackage().getName();

  /**
   * Static instance of the configuration
   */
  protected static LoggingConfiguration config = new LoggingConfiguration(confbase, CLIConffile);

  /**
   * Configure Java Logging API: {@link java.util.logging}
   */
  private LoggingConfiguration(final String pkg, final String name) {
    privateReconfigureLogging(pkg, name);
  }

  /**
   * Reconfigure logging.
   * 
   * @param pkg Package name the configuration file comes from
   * @param name File name.
   */
  public static void reconfigureLogging(final String pkg, final String name) {
    config.privateReconfigureLogging(pkg, name);
  }

  /**
   * Reconfigure logging.
   * 
   * @param pkg Package name the configuration file comes from
   * @param name File name.
   */
  private void privateReconfigureLogging(String pkg, final String name) {
    LogManager logManager = LogManager.getLogManager();
    Logger logger = Logger.getLogger(LoggingConfiguration.class.getName());
    // allow null as package name.
    if(pkg == null) {
      pkg = "";
    }
    // Load logging configuration from resources.
    String cfgfile = pkg.replace('.', File.separatorChar) + File.separatorChar + name;
    try {
      InputStream cfgdata = FileUtil.openSystemFile(cfgfile);
      logManager.readConfiguration(cfgdata);

      // also load as properties for us, to get debug flag.
      InputStream cfgdata2 = FileUtil.openSystemFile(cfgfile);
      Properties cfgprop = new Properties();
      cfgprop.load(cfgdata2);
      DEBUG = Boolean.valueOf(cfgprop.getProperty("debug"));

      logger.info("Logging configuration read.");
    }
    catch(FileNotFoundException e) {
      logger.log(Level.SEVERE, "Could not find logging configuration file: " + cfgfile, e);
    }
    catch(Exception e) {
      logger.log(Level.SEVERE, "Failed to configure logging from file: " + cfgfile, e);
    }
  }

  /**
   * Assert that logging was configured.
   */
  public static void assertConfigured() {
    // nothing happening here, just to ensure static construction was run.
  }

  /**
   * Reconfigure logging to enable 'verbose' logging at the top level.
   * 
   * @param verbose verbose flag
   */
  public static void setVerbose(boolean verbose) {
    Logger logger = Logger.getLogger(TOPLEVEL_PACKAGE);
    if(verbose) {
      // decrease to INFO if it was higher
      if(logger.getLevel() == null || logger.getLevel().intValue() > Level.INFO.intValue()) {
        logger.setLevel(Level.INFO);
      }
    }
    else {
      // increase to warning level if it was INFO.
      if(logger.getLevel() != null || logger.getLevel() == Level.INFO) {
        logger.setLevel(Level.WARNING);
      }
    }
  }

  /**
   * Add a handler to the root logger.
   * 
   * @param handler Handler
   */
  public static void addHandler(Handler handler) {
    LogManager.getLogManager().getLogger("").addHandler(handler);
  }

  /**
   * Replace the default log handler with the given log handler.
   * 
   * This will remove all {@link CLISmartHandler} found on the root logger. It
   * will leave any other handlers in place.
   * 
   * @param handler Logging handler.
   */
  public static void replaceDefaultHandler(Handler handler) {
    Logger rootlogger = LogManager.getLogManager().getLogger("");
    for(Handler h : rootlogger.getHandlers()) {
      if(h instanceof CLISmartHandler) {
        rootlogger.removeHandler(h);
      }
    }
    addHandler(handler);
  }

  /**
   * Set the logging level for a particular package/class.
   * 
   * @param pkg Package
   * @param level Level name
   * @throws IllegalArgumentException thrown when logger or level was not found
   */
  public static void setLevelFor(String pkg, String level) throws IllegalArgumentException {
    Logger logr = Logger.getLogger(pkg);
    if(logr == null) {
      throw new IllegalArgumentException("Logger not found.");
    }
    // Can also throw an IllegalArgumentException
    Level lev = Level.parse(level);
    logr.setLevel(lev);
  }
}