package de.lmu.ifi.dbs.logging;
/**
 * A filter for message logs - suitable for handling messages.
 * A LogRecord is treated as loggable if its level is LogLevel.Message.
 * 
 * @author Steffi Wanka
 *
 */
public class MessageFilter extends SelectiveFilter {

	/**
	 * Provides a filter for message logs.
	 *
	 */
	public MessageFilter(){
		super(LogLevel.MESSAGE);
	}
}
