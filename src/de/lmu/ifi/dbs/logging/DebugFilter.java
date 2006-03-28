package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;

/**
 * A filter for all (or specified) logs - suitable for handling debugging messages.
 * A LogRecord is treated as loggable if its level is at least
 * the currently specified debug level.
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
     * 
     * 
     * @param level the level for filtering debug messages.
     */
    public void setDebugLevel(Level level)
    {
        this.selectedLevel = level;
    }

}
