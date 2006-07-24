package de.lmu.ifi.dbs.logging;

import java.util.logging.LogRecord;

import de.lmu.ifi.dbs.utilities.Progress;

/**
 * Interface providing the methods required to log messages according to {@link #LogLevel} levels.
 * 
 * @author Steffi Wanka
 *
 */
public interface Loggable {

	/**
	 * Log an EXCEPTION message.
	 * 
	 * @param msg The String message
	 */
	void exception(String msg, Throwable e);
	
	/**
	 * Log a WARNING message:
	 * @param msg The String message
	 */
	void warning(String msg);
	
	/**
	 * Log a MESSAGE.
	 * @param msg The String message
	 */
	void message(String msg);
	
	/**
	 * Log a PROGRESS message.
	 * @param pgr The Progress to be logged
	 */
	void progress(Progress pgr);
	
	
	/**
	 * Log a PROGRESS message.
	 * 
	 * @param record the log record to be logged
	 */
	void progress(LogRecord record);
	
	/**
	 * Log a VERBOSE message.
	 * @param msg The String message.
	 */
	void verbose(String msg);
	
	/**
	 * Log a DEBUG_FINE message. 
	 * @param msg The String message
	 */
	void debugFine(String msg);
	
	/**
	 * Log a DEBUG_FINER message.
	 * @param msg The String message
	 */
	void debugFiner(String msg);
	
	/**
	 * Log a DEBUG_FINEST message.
	 * @param msg The String message
	 */
	void debugFinest(String msg);
}
