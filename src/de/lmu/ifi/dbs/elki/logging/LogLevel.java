package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Level;

/**
 * This class provides a set of logging levels that should be used to control logging output. 
 * @see java.util.logging.Level
 * 
 * @author Steffi Wanka
 *
 */
public class LogLevel extends java.util.logging.Level {
  /**
   * Serial version UID
   */
  private static final long serialVersionUID = -2377534728073617317L;

  /**
   * Bundle name to use
   */
  private final static String elkiBundle = LogLevel.class.getPackage().getName();

  /**
	 * Constructs a log level with the given name and the given integer value.
	 * 
	 * @param name the name of the level.
	 * @param value the value of the level.
	 */
	public LogLevel(String name, int value) {
		super(name,value, elkiBundle);
	}
	
	/**
	 * Assert that the class was initialized, and that the levels are registered with the LogManager
	 */
	public static void assertLevelsLoaded() {
	  // this is to ensure the levels have been initialized.
	  PROGRESS.getName();
	}
	
	/**
	 * PROGRESS is a message level describing progress messages.
	 */
	public static final Level PROGRESS = new LogLevel("PROGRESS", Level.INFO.intValue() + 1);
	
	/**
	 * VERBOSE is a message level providing regular user information.
	 */
	public static final Level VERBOSE = new LogLevel("VERBOSE", Level.INFO.intValue() - 1);
}
