package de.lmu.ifi.dbs.logging;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;

public abstract class AbstractLoggable implements Loggable {

	/**
	 * Holds the class specific debug status.
	 */
	protected boolean debug;

	/**
	 * The logger of the class.
	 */
	private final Logger logger;

	public AbstractLoggable(boolean debug) {

		this.logger = Logger.getLogger(this.getClass().getName());
		this.debug = debug;
	}

	
	
	
	/**
	 * Log an EXCEPTION message. * *
	 * <p>
	 * If the logger is currently enabled for the EXCEPTION message level then
	 * the given message is forwarded to all the registered output Handler
	 * objects.
	 * <p>
	 * 
	 * @param msg
	 *            The String message
	 */
	public void exception(String msg,Throwable e) {
		logger.log(LogLevel.EXCEPTION, msg, e);
	}

	/**
	 * Log a WARNING message. * *
	 * <p>
	 * If the logger is currently enabled for the WARNING message level then the
	 * given message is forwarded to all the registered output Handler objects.
	 * <p>
	 * 
	 * @param msg
	 *            The String message
	 */
	public void warning(String msg) {
		logger.log(LogLevel.WARNING, msg);
	}

	/**
	 * Log a MESSAGE. * *
	 * <p>
	 * If the logger is currently enabled for the MESSAGE level then the given
	 * message is forwarded to all the registered output Handler objects.
	 * <p>
	 * 
	 * @param msg
	 *            The String message
	 */
	public void message(String msg) {
		logger.log(LogLevel.MESSAGE, msg);
	}

	/**
	 * Log a PROGRESS message. *
	 * <p>
	 * If the logger is currently enabled for the PROGRESS message level then
	 * the given message is forwarded to all the registered output Handler
	 * objects.
	 * <p>
	 * 
	 * @param pgr
	 *            The Progress to be logged.
	 */
	public void progress(Progress pgr) {

		logger.log(new ProgressLogRecord(LogLevel.PROGRESS, Util.status(pgr)
				+ "\n", pgr.getTask(), pgr.status()));
	}

	
	public void progress(LogRecord record){
		logger.log(record);
	}
	
	/**
	 * Log a VERBOSE message.
	 * <p>
	 * If the logger is currently enabled for the VERBOSE message level then the
	 * given message is forwarded to all the registered output Handler objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message 
	 */
	public void verbose(String msg) {

		
		logger.log(LogLevel.VERBOSE, msg+"\n");
	}

	/**
	 * Log a DEBUG_FINE message.
	 * <p>
	 * If the logger is currently enabled for the DEBUG_FINE message level then
	 * the given message is forwarded to all the registered output Handler
	 * objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message
	 */
	public void debugFine(String msg) {

		LogRecord record = new LogRecord(LogLevel.DEBUG_FINE, msg);
		record.setSourceClassName(this.getClass().getName());
		record.setSourceMethodName(inferCaller(this.getClass().getName()));
		logger.log(record);
	}

	/**
	 * Log a DEBUG_FINER message.
	 * <p>
	 * If the logger is currently enabled for the DEBUG_FINER message level then
	 * the given message is forwarded to all the registered output Handler
	 * objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message
	 */
	public void debugFiner(String msg) {

		LogRecord record = new LogRecord(LogLevel.DEBUG_FINER, msg);
		record.setSourceClassName(this.getClass().getName());
		record.setSourceMethodName(inferCaller(this.getClass().getName()));
		logger.log(record);
	}

	/**
	 * Log a DEBUG_FINEST message.
	 * <p>
	 * If the logger is currently enabled for the DEBUG_FINEST message level
	 * then the given message is forwarded to all the registered output Handler
	 * objects.
	 * <p>
	 * 
	 * @param msg
	 *            The string message (or a key in the message catalog)
	 */
	public void debugFinest(String msg) {

		LogRecord record = new LogRecord(LogLevel.DEBUG_FINEST, msg);
		record.setSourceClassName(this.getClass().getName());
		record.setSourceMethodName(inferCaller(this.getClass().getName()));
		logger.log(record);
	}

	private String inferCaller(String className) {

		String methodName = null;
		StackTraceElement stack[] = (new Throwable()).getStackTrace();
		int ix = 0;
		while (ix < stack.length) {
			StackTraceElement frame = stack[ix];

			if (frame.getClassName().equals(className)) {
				return frame.getMethodName();
			}
			ix++;
		}

		return methodName;
	}

}
