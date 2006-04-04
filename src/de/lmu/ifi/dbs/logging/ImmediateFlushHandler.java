package de.lmu.ifi.dbs.logging;

import java.io.OutputStream;
import java.util.logging.Filter;
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
     * immediately to the designated OutputStream.
     * 
     * The handler sets the given filter and the handler's level reflects
     * the filter's level.
     * 
     * @param out the OutputStream to publish LogRecords to
     * @param formatter a formatter to format LogRecords for publishing
     * @param filter a filter specifying the minimum level for this handler
     * 
     * @see StreamHandler#StreamHandler()
     */
    public ImmediateFlushHandler(OutputStream out, Formatter formatter, SelectiveFilter filter)
    {
        super(out, formatter);
        super.setFilter(filter);
        super.setLevel(filter.getLevel());
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
