package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A selective filter filters exactly for a certain {@link LogLevel LogLevel}
 * of {@link LogRecord LogRecord}s.
 *
 * @author Arthur Zimek
 */
public class SelectiveFilter extends AbstractLoggable implements Filter {
    /**
     * The level to filter for.
     */
    private Level selectedLevel;

    /**
     * Provides a selective filter for the given level.
     *
     * @param selectedLevel the level to filter for
     */
    protected SelectiveFilter(Level selectedLevel) {
        super(LoggingConfiguration.DEBUG);
        this.selectedLevel = selectedLevel;
    }

    /**
     * Sets the selected level
     * to the specified level.
     *
     * @param selectedLevel the level to filter for
     */
    public void setLevel(Level selectedLevel) {
        this.selectedLevel = selectedLevel;
    }

    /**
     * Returns the currently set level of selection.
     *
     * @return the currently set level of selection
     */
    public Level getLevel() {
        return selectedLevel;
    }

    /**
     * Decides whether or not the given LogRecord is loggable.
     * Generally, a LogRecord is loggable
     * iff the level of <code>record</code>
     * equals the
     * {@link #selectedLevel selectedLevel}.
     * More generous decisions may be implemented by extending classes.
     *
     * @return true if the level of <code>record</code>
     *         equals the
     *         {@link #selectedLevel selectedLevel},
     *         false otherwise
     * @see LogRecord#equals(Object)
     */
    public boolean isLoggable(LogRecord record) {
        return record.getLevel().equals(selectedLevel);
    }

}
