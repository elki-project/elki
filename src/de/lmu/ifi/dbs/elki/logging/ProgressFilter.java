package de.lmu.ifi.dbs.elki.logging;
/**
 * A filter for progress logs - suitable for handling progress messages.
 * A LogRecord is treated as loggable if its level is LogLevel.PROGRESS.
 * 
 * @author Steffi Wanka
 *
 */
public class ProgressFilter extends SelectiveFilter {

	/**
	 * Provides a filter for progress logs.
	 *
	 */
	public ProgressFilter(){
		super(LogLevel.PROGRESS);
	}
}
