package de.lmu.ifi.dbs.logging;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ImmediateFlushHandler extends StreamHandler
{

    
    
    /**
     * 
     */
    public ImmediateFlushHandler()
    {
        super();
    }

    /**
     * @param out
     * @param formatter
     */
    public ImmediateFlushHandler(OutputStream out, Formatter formatter)
    {
        super(out, formatter);
    }

    @Override
    public synchronized void publish(LogRecord record)
    {
        super.publish(record);
        super.flush();
    }
    
}
