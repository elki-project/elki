package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;

/**
 * A filter for warning logs - suitable for handling warning messages.
 * A LogRecord is treated as loggable if its level is Level.WARNING.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class WarningFilter extends SelectiveFilter
{
    public WarningFilter()
    {
        selectedLevel = Level.WARNING;
    }
    
}
