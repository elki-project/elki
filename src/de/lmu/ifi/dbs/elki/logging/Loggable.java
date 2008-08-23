package de.lmu.ifi.dbs.elki.logging;

import de.lmu.ifi.dbs.elki.utilities.Progress;

import java.util.logging.LogRecord;

/**
 * Interface providing the methods required to log messages according to {@link LogLevel} levels.
 * 
 * @author Steffi Wanka
 *
 */
public interface Loggable {

	/**
	 * Log an EXCEPTION message.
	 * 
	 * @param msg the string message
     * @param e the exception
	 */
	void exception(String msg, Throwable e);
	
	/**
	 * Log a WARNING message:
	 * @param msg the string message
	 */
	void warning(String msg);
	
	/**
	 * Log a MESSAGE.
	 * @param msg the string message
	 */
	void message(String msg);
	
	/**
	 * Log a PROGRESS message.
	 * @param pgr the progress to be logged
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
	 * @param msg the string message
	 */
	void verbose(String msg);
    
    /**
     * Log an empty VERBOSE message.
     * 
     * This method should insert a newline in the verbose-log.
     */
    void verbose();
	
	/**
	 * Log a DEBUG_FINE message. 
	 * @param msg the string message
	 */
	void debugFine(String msg);
	
	/**
	 * Log a DEBUG_FINER message.
	 * @param msg the string message
	 */
	void debugFiner(String msg);
	
	/**
	 * Log a DEBUG_FINEST message.
	 * @param msg the string message
	 */
	void debugFinest(String msg);
}
