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

    
    public DebugFilter()
    {
        this.selectedLevel = Level.ALL;
    }
    
    public DebugFilter(Level debugLevel)
    {
        this.selectedLevel = debugLevel;
    }
    
    /**
     * Sets the debug level for filtering to the specified level.
     * If the given level is above {@link Level#FINE FINE},
     * no messages will be treated as loggable.
     * 
     * @param level the level for filtering debug messages,
     * should usually be one of {@link Level#FINE FINE}, {@link Level#FINER FINER},
     * or {@link Level#FINEST FINEST}.
     */
    public void setDebugLevel(Level level)
    {
        this.selectedLevel = level;
    }

    /**
     * A LogRecord is loggable if it is at least of the currently selected
     * debug level, but at most of level {@link Level#FINE FINE}.
     * 
     * @see de.lmu.ifi.dbs.logging.SelectiveFilter#isLoggable(java.util.logging.LogRecord)
     */
    @Override
    public boolean isLoggable(LogRecord record)
    {
        return record.getLevel().intValue() >= selectedLevel.intValue()
            && record.getLevel().intValue() <= Level.FINE.intValue();
    }

    
}
