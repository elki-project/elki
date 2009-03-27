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
	 * EXCEPTION is a message level indicating a serious failure.
	 */
	public static final Level EXCEPTION = new LogLevel("EXCEPTION", Level.SEVERE.intValue()+1);
	
	/**
	 * MESSAGE is a message level describing common messages to keep the user informed of the status of the program.
	 */
	public static final Level MESSAGE = new LogLevel("MESSAGE", Level.INFO.intValue() + 2);
	
	/**
	 * PROGRESS is a message level describing progress messages.
	 */
	public static final Level PROGRESS = new LogLevel("PROGRESS", Level.INFO.intValue() + 1);
	
	/**
	 * VERBOSE is a message level providing regular user information.
	 */
	public static final Level VERBOSE = new LogLevel("VERBOSE", Level.INFO.intValue() - 1);
}
