package de.lmu.ifi.dbs.elki.logging.progress;

import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.logging.ELKILogRecord;

/**
 * Log record for progress messages.
 * 
 * @author Erich Schubert
 */
public class ProgressLogRecord extends ELKILogRecord {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Progress storage
   */
  private final Progress progress;

  /**
   * Constructor for progress log messages.
   * 
   * @param level Logging level
   * @param msg Logging message
   * @param progress Progress to log
   */
  public ProgressLogRecord(Level level, String msg, Progress progress) {
    super(level, msg, true);
    this.progress = progress;
  }

  /**
   * @return the progress
   */
  protected Progress getProgress() {
    return progress;
  }

}
