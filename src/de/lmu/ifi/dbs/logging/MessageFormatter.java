package de.lmu.ifi.dbs.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter to simply retrieve the message of an LogRecord.
 * 
 * @author Arthur Zimek
 */
public class MessageFormatter extends Formatter
{
    /**
     * Provides a message formatter
     * to simply retrieve the message of an LogRecord.
     */
    public MessageFormatter()
    {
        super();
    }

    /**
     * Retrieves the message as it is set in the given LogRecord.
     * 
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record)
    {
        return record.getMessage();
    }

}
