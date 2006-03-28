package de.lmu.ifi.dbs.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class SelectiveFilter implements Filter
{
    protected Level selectedLevel;
    
    public Level getLevel()
    {
        return selectedLevel;
    }

    /**
     * 
     * 
     * @return true if the level of <code>record</code> is the
     * {@link #selectedLevel selected level},
     * false otherwise
     * 
     * @see java.util.logging.Filter#isLoggable(java.util.logging.LogRecord)
     */
    public boolean isLoggable(LogRecord record)
    {
        return record.getLevel().equals(selectedLevel);
    }

}
