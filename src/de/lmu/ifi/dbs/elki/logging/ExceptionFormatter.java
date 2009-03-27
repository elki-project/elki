package de.lmu.ifi.dbs.elki.logging;


import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter to format exception messages.
 *
 * @author Arthur Zimek
 */
public class ExceptionFormatter extends Formatter {
    /**
     * Holds the status of requests for printing the stack trace.
     * In this case, the status true is required
     * for complete view of exceptions.
     * If <code>showStacktrace</code> is <code>false</code>,
     * only the exception message is printed.
     * Otherwise, the user gets information
     * w.r.t. cause and stack trace.
     */
    private boolean showStackTrace;

    /**
     * Provides an exception formatter
     * for exception messages.
     */
    public ExceptionFormatter(boolean showStackTrace) {
        super();
        this.showStackTrace = showStackTrace;
        //try
        //{
        //  showStackTrace = Boolean.valueOf(Properties.ELKI_PROPERTIES.getProperty(PropertyName.STACK_TRACE_CLI)[0]);
        //}
        //catch (Exception e) {
        //  // in this case, the properties are not yet initialized, showStackTrace just remains false as default
        //}
    }

    /**
     * Exception messages are formatted dependent from the
     * debug mode as assigned by DEBUG.
     * <ul>
     * <li>If debug mode is disabled (i.e. <code>DEBUG=false</code>)
     * the regular user information will be provided:
     * i.e., the message of the LogEntry only.
     * </li>
     * <li>In debug mode, one more detailed
     * information is provided, as the name
     * of the causing exception and the stack trace.
     * </li>
     * </ul>
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder exceptionMessage = new StringBuilder();
        Throwable cause = record.getThrown();
        exceptionMessage.append("EXCEPTION: ");
        if (cause != null) {
            exceptionMessage.append(cause.getClass().getName());
        }
        else {
            exceptionMessage.append("unknown exception");
        }
        exceptionMessage.append('\n');
        
        exceptionMessage.append(record.getMessage());
        exceptionMessage.append('\n');
        if (showStackTrace) {
            if (cause != null) {
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

    public String getStackTrace(LogRecord record) {
        Throwable cause = record.getThrown();
        if (cause != null) {
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement e : cause.getStackTrace()) {
                stackTrace.append(e.toString());
                stackTrace.append('\n');
            }
            return stackTrace.toString();
        }
        else {
            return "unknown";
        }
    }
}
