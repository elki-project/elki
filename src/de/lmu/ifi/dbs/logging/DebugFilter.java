package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A filter for all (or specified) logs - suitable for handling debugging messages.
 * A LogRecord is treated as loggable if its level is at least
 * the currently specified debug level, but at most level {@link Level#FINE FINE}.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DebugFilter extends SelectiveFilter
{
//    /**
//     * Holds the class specific debug status.
//     */
//    @SuppressWarnings("unused")
//    private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//
//    /**
//     * The logger of this class.
//     */
//    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Provides a debug filter for all levels
     * below {@link Level#INFO INFO}.
     *
     */
    public DebugFilter()
    {
        super(LogLevel.ALL);
    }
    
    /**
     * Provides a debug filter for all levels
     * below {@link Level#INFO INFO}
     * and above the specified debugLevel.
     * 
     * @param debugLevel the lowest interesting debug level
     */
    public DebugFilter(Level debugLevel)
    {
        super(debugLevel);
    }
    
    /**
     * Sets the debug level for filtering to the specified level.
     * If the given level is above {@link LogLevel#DEBUG_FINE DEBUG_FINE},
     * no messages will be treated as loggable.
     * 
     * @param level the level for filtering debug messages,
     * should usually be one of {@link LogLevel#DEBUG_FINE DEBUG_FINE},
     * {@link LogLevel#DEBUG_FINER DEBUG_FINER},
     * or {@link LogLevel#DEBUG_FINEST DEBUG_FINEST}.
     */
    public void setLevel(Level level)
    {
        if(level.intValue() > LogLevel.DEBUG_FINE.intValue())
        {
            warning("debug level set to "+level.toString()+" - no debug messages will be logged.\n");
        }
        super.setLevel(level);
    }

    /**
     * A LogRecord is loggable if it is at least of the currently selected
     * debug level, but at most of level {@link LogLevel#DEBUG_FINE DEBUG_FINE}.
     * 
     * @see de.lmu.ifi.dbs.logging.SelectiveFilter#isLoggable(java.util.logging.LogRecord)
     */
    @Override
    public boolean isLoggable(LogRecord record)
    {
      // todo <= FINE!
        return record.getLevel().intValue() >= getLevel().intValue()
            && record.getLevel().intValue() < LogLevel.DEBUG_FINE.intValue();
    }

    
}
