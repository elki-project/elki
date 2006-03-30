package de.lmu.ifi.dbs.logging;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * ImmediateFlushHandler is a stream handler flushing each published LogRecord
 * immediately.
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ImmediateFlushHandler extends StreamHandler
{

    
    
    /**
     * Provides a stream handler flushing each published LogRecord
     * immediately, but no currently set OutputStream.
     * @see StreamHandler#StreamHandler()
     */
    public ImmediateFlushHandler()
    {
        super();
    }

    /**
     * Provides a stream handler flushing each published LogRecord
     * immediately to the designated OutputStream.
     * 
     * @param out the OutputStream to publish LogRecords to
     * @param formatter a formatter to format LogRecords for publishing
     * @see StreamHandler#StreamHandler(OutputStream, Formatter)
     */
    public ImmediateFlushHandler(OutputStream out, Formatter formatter)
    {
        super(out, formatter);
    }

    /**
     * Publishs the given LogRecord and flushs immediately.
     * 
     * @see java.util.logging.StreamHandler#publish(java.util.logging.LogRecord)
     */
    @Override
    public synchronized void publish(LogRecord record)
    {
        super.publish(record);
        super.flush();
    }
    
}
