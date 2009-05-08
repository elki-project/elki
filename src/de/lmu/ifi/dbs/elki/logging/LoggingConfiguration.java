package de.lmu.ifi.dbs.elki.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
  protected static LoggingConfiguration config = new LoggingConfiguration();

  /**
   * Configure Java Logging API: {@link java.util.logging}
   */
  private LoggingConfiguration() {
    LogManager logManager = LogManager.getLogManager();
    Logger logger = Logger.getLogger(LoggingConfiguration.class.getName());

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
    loadConfigurationPrivate(confbase, CLIConffile);
  }

  /**
   * Load a configuration file.
   * 
   * @param pkg Package name to look in
   * @param name File name
   */
  private void loadConfigurationPrivate(final String pkg, final String name) {
    final Logger logger = Logger.getLogger(LoggingConfiguration.class.getName());
    final String base = (pkg == null) ? "" : pkg;
    if(name == null) {
      logger.log(Level.SEVERE, "No configuration file name given.");
      return;
    }
    // extra config settings
    // try in the file system first
    File cfgfile = new File(base.replace('.', File.separatorChar) + File.separatorChar + name);
    Properties cfgprop = new Properties();
    try {
      InputStream res = ClassLoader.getSystemResourceAsStream(base.replace('.', '/') + '/' + name);
      if(res != null) {
        cfgprop.load(res);
      }
      if(cfgfile.exists() && cfgfile.canRead()) {
        cfgprop.load(new FileInputStream(cfgfile.getAbsolutePath()));
      }
    }
    catch(Exception e) {
      logger.log(Level.SEVERE, "Failed to load logging properties from " + cfgfile.getAbsolutePath(), e);
    }
    DEBUG = Boolean.valueOf(cfgprop.getProperty("debug"));
  }

  /**
   * Reconfigure logging using a configuration file.
   * 
   * @param pkg Package name, may be null
   * @param name File name, may not be null
   */
  public static void loadConfiguration(final String pkg, final String name) {
    config.loadConfigurationPrivate(pkg, name);
  }

  /**
   * Assert that logging was configured.
   */
  public static void assertConfigured() {
    // nothing happening here, just to ensure static construction was run.
  }

  /**
   * Reconfigure logging to enable 'verbose' logging at the top level.
   * @param verbose verbosity flag
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
}
