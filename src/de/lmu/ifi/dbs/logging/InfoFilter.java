package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;

/**
 * A filter for info logs - suitable for handling verbose messages.
 * A LogRecord is treated as loggable if its level is Level.INFO.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class InfoFilter extends SelectiveFilter
{
    public InfoFilter()
    {
        this.selectedLevel = Level.INFO;
    }

}
