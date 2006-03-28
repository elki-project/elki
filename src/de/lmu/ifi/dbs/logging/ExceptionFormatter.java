package de.lmu.ifi.dbs.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ExceptionFormatter extends Formatter
{
    /**
     * Holds the class specific debug status.
     */
    private static final boolean DEBUG = true;

    /**
     * 
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record)
    {
        StringBuilder exceptionMessage = new StringBuilder();
        exceptionMessage.append("EXCEPTION:\n");
        exceptionMessage.append(record.getMessage());
        exceptionMessage.append('\n');
        if(DEBUG)
        {
            exceptionMessage.append("\nCaused by:\n");
            exceptionMessage.append(getStackTrace(record));
            exceptionMessage.append('\n');
        }
        return exceptionMessage.toString();
    }
    
    public String getStackTrace(LogRecord record)
    {
        Throwable cause = record.getThrown();
        if(cause != null)
        {
            StringBuilder stackTrace = new StringBuilder();
            for(StackTraceElement e : cause.getStackTrace())
            {
                stackTrace.append(e.toString());
                stackTrace.append('\n');
            }
            return stackTrace.toString();
        }
        else
        {
            return "";
        }
    }

}
