package de.lmu.ifi.dbs.logging;


/**
 * A filter for info logs - suitable for handling verbose messages. A LogRecord
 * is treated as loggable if its level is LogLevel.VERBOSE.
 * 
 * @author Arthur Zimek 
 */
public class InfoFilter extends SelectiveFilter
{
    /**
     * Provides a filter for verbose messages.
     *
     */
    public InfoFilter()
    {
        super(LogLevel.VERBOSE);
    }

}
