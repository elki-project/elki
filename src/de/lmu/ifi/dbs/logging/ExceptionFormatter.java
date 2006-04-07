package de.lmu.ifi.dbs.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter to format exception messages.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ExceptionFormatter extends Formatter
{
    /**
     * Holds the class specific debug status.
     * In this case, the status true is required
     * for complete view of exceptions.
     * If <code>DEBUG</code> is <code>false</code>,
     * only the exception message is printed.
     * Otherwise, the user gets information
     * w.r.t. cause and stacktrace.
     */
    private static final boolean DEBUG = true;
    // XXX note to set this variable accordingly
    // experimental version: false
    // release version:
    // LoggingConfiguration.DEBUG;

    /**
     * Provides an exception formatter
     * for exception messages.
     *
     */
    public ExceptionFormatter()
    {
        super();
    }
    
    /**
     * Exception messages are formatted dependent from the
     * debug mode as assigned by {@link #DEBUG DEBUG}.
     * <ul>
     * <li>If debug mode is deactivated (i.e. {@link #DEBUG DEBUG}<code>=false</code>)
     *     the regular user information will be provided:
     *     i.e., the message of the LogEntry only.
     * </li>
     * <li>In debug mode, one more detailed
     *     information is provided, as the name
     *     of the causing exception and the stacktrace.
     * </li>
     * </ul>
     * Current status: {@value #DEBUG}.
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record)
    {
        StringBuilder exceptionMessage = new StringBuilder();
        Throwable cause = record.getThrown();
        if(DEBUG)
        {
            exceptionMessage.append("EXCEPTION: ");            
            if(cause != null)
            {         
                exceptionMessage.append(cause.getClass().getName());
            }
            else
            {
                exceptionMessage.append("unknown exception");
            }
            exceptionMessage.append('\n');
        }        
        exceptionMessage.append(record.getMessage());
        exceptionMessage.append('\n');
        if(DEBUG)
        {
            if(cause != null)
            {
                exceptionMessage.append("\nCaused by:\n");
                exceptionMessage.append(cause.toString());
                exceptionMessage.append("\n");
            }
            exceptionMessage.append("\nStack Trace:\n");
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
            return "unknown";
        }
    }

}
