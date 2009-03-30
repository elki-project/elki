package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Level;

/**
 * A filter for all (or specified) logs - suitable for handling debugging messages.
 * A LogRecord is treated as loggable if its level is at least
 * the currently specified debug level, but at most level {@link LogLevel#FINE FINE}.
 *
 * @author Arthur Zimek
 */
public class DebugFilter extends SelectiveFilter {
    /**
     * Provides a debug filter for all levels
     * below {@link LogLevel#VERBOSE VERBOSE}
     * and above the specified debugLevel.
     *
     * @param debugLevel the lowest interesting debug level
     */
    public DebugFilter(Level debugLevel) {

        super(debugLevel, LogLevel.VERBOSE);
    }

    /**
     * Sets the debug level for filtering to the specified level.
     * If the given level is above {@link LogLevel#FINE FINE},
     * no messages will be treated as loggable.
     *
     * @param level the level for filtering debug messages,
     *              should usually be one of {@link LogLevel#FINE FINE},
     *              {@link LogLevel#FINER FINER},
     *              or {@link LogLevel#FINEST FINEST}.
     */
    @Override
    public void setMinLevel(Level level) {
        if (level.intValue() > LogLevel.FINE.intValue()) {
            warning("debug level set to " + level.toString() + " - no debug messages will be logged.\n");
        }
        super.setMinLevel(level);
    }
}
