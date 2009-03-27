package de.lmu.ifi.dbs.elki.logging;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * ImmediateFlushHandler is a stream handler
 * flushing each published LogRecord
 * immediately.
 *
 * @author Arthur Zimek
 */
public class ImmediateFlushHandler extends StreamHandler {


    /**
     * Provides a stream handler flushing each published LogRecord
     * immediately to the designated OutputStream.
     * <p/>
     * The handler sets the given filter and the handler's level reflects
     * the filter's level.
     *
     * @param out       the OutputStream to publish LogRecords to
     * @param formatter a formatter to format LogRecords for publishing
     * @param filter    a filter specifying the minimum level for this handler
     * @see StreamHandler#StreamHandler(java.io.OutputStream,java.util.logging.Formatter)
     */
    public ImmediateFlushHandler(OutputStream out, Formatter formatter, SelectiveFilter filter) {
        super(out, formatter);
        if (filter != null) {
          super.setFilter(filter);
        }
        super.setLevel(filter.getLevel());
    }


    /**
     * The given LogRecord is loggable if it is not null
     * and the filter of this handler would treat it as loggable.
     */
    @Override
    public boolean isLoggable(LogRecord record) {
        if (record == null) {
            return false;
        }
        else {
            return getFilter().isLoggable(record);
        }
    }


    /**
     * Publishes the given LogRecord and flushes immediately.
     */
    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        super.flush();
    }

}
