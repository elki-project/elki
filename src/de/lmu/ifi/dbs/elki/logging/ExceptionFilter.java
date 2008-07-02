package de.lmu.ifi.dbs.elki.logging;


/**
 * A filter for exception logs - suitable for handling severe error messages. A
 * LogRecord is treated as loggable if its level is Level.SEVERE.
 * 
 * @author Arthur Zimek 
 */
public class ExceptionFilter extends SelectiveFilter
{

    /**
     * Provides a filter for exception logs
     * (LogRecords of level {@link LogLevel#EXCEPTION EXCEPTION}).
     *
     */
    public ExceptionFilter()
    {
        super(LogLevel.EXCEPTION);
    }

}
