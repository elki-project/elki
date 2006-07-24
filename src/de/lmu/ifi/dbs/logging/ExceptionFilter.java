package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;

/**
 * A filter for exception logs - suitable for handling severe error messages. A
 * LogRecord is treated as loggable if its level is Level.SEVERE.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
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
