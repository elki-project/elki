package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;


/**
 * This class provides a set of logging levels that should be used to control logging output. 
 * @see java.util.logging.Level
 * 
 * @author Steffi Wanka
 *
 */
public class LogLevel extends Level {

	

	public LogLevel(String name, int value){
	
		super(name,value);
	}
	
	
	/**
	 * EXCEPTION is a message level indicating a serious failure.
	 * <p>
     * This level is initialized to <CODE>1797</CODE>.
     * </p>
	 */
	public static final LogLevel EXCEPTION = new LogLevel("EXCEPTION", 1797);
	
	
	
	
	/**
	 * WARNING is a message level indicating warning messages.
	 * <p>
     * This level is initialized to <CODE>1697</CODE>.
     * </p>
	 */
	public static final LogLevel WARNING = new LogLevel("WARNING",1697);
	
	/**
	 * MESSAGE is a message level describing 
	 * <p>
     * This level is initialized to <CODE>1597</CODE>.
     * </p>
	 */
	public static final LogLevel MESSAGE = new LogLevel("MESSAGE",1597);
	
	
	/**
	 * PROGRESS is a message level describing progress messages.
	 * <p>
     * This level is initialized to <CODE>1497</CODE>.
     * </p>
	 */
	public static final LogLevel PROGRESS = new LogLevel("PROGRESS",1497);
	
	
	/**
	 * VERBOSE is a message level providing regular user information.
	 * <p>
     * This level is initialized to <CODE>1397</CODE>.
     * </p>
	 */
	public static final LogLevel VERBOSE = new LogLevel("VERBOSE",1397);
	
	/**
	 * DEBUG_FINE is a message level providing debugging messages. 
	 * <p>
     * This level is initialized to <CODE>1297</CODE>.
     * </p>
	 */
	public static final LogLevel DEBUG_FINE = new LogLevel("DEBUG_FINE",1297);
	
	/**
	 * DEBUG_FINER is a message level providing fairly detailed debugging messages.
	 * <p>
     * This level is initialized to <CODE>1197</CODE>.
     * </p>
	 */
	public static final LogLevel DEBUG_FINER = new LogLevel("DEBUG_FINER",1197);
	
	/**
	 * DEBUG_FINEST is a message level providing highly detailed debugging messages.
	 * <p>
     * This level is initialized to <CODE>1097</CODE>.
     * </p>
	 */
	public static final LogLevel DEBUG_FINEST = new LogLevel("DEBUG_FINEST", 1097);
	
	
}
