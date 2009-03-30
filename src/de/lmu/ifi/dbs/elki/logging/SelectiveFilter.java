package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A selective filter filters exactly for a certain {@link LogLevel LogLevel} of
 * {@link LogRecord LogRecord}s.
 * 
 * @author Arthur Zimek
 */
public class SelectiveFilter extends AbstractLoggable implements Filter {
  /**
   * Minimum level to print messages for.
   */
  private Level minLevel;

  /**
   * Maximum level to print messages for.
   */
  private Level maxLevel;

  /**
   * Provides a selective filter for the given level.
   * 
   * @param selectedLevel the level to filter for
   */
  protected SelectiveFilter(Level selectedLevel) {
    super(LoggingConfiguration.DEBUG);
    this.minLevel = selectedLevel;
    this.maxLevel = selectedLevel;
  }

  /**
   * Provides a selective filter for the given level.
   * 
   * @param minLevel the level to start filtering
   * @param maxLevel the level to stop filtering at
   */
  protected SelectiveFilter(Level minLevel, Level maxLevel) {
    super(LoggingConfiguration.DEBUG);
    this.minLevel = minLevel;
    this.maxLevel = maxLevel;
  }

  /**
   * Sets the selected level to the specified level.
   * 
   * @param selectedLevel the level to filter for
   */
  public void setMinLevel(Level selectedLevel) {
    this.minLevel = selectedLevel;
  }

  /**
   * Decides whether or not the given LogRecord is loggable. Generally, a
   * LogRecord is loggable iff the level of <code>record</code> if its level
   * is between {@link #minLevel} and {@link #maxLevel} (not inclusive).
   * 
   * @return true if the level of <code>record</code> matches the given range
   * @see LogRecord#equals(Object)
   */
  public boolean isLoggable(LogRecord record) {
    int level = record.getLevel().intValue();
    if (level >= maxLevel.intValue()) {
      return (level == minLevel.intValue());
    }
    return (level >= minLevel.intValue());
  }

  public Level getMinLevel() {
    return minLevel;
  }

}
