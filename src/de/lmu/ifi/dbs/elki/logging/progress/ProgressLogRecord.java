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
   * @param progress Progress to log
   */
  public ProgressLogRecord(Level level, Progress progress) {
    super(level, null);
    this.progress = progress;
  }

  /**
   * Get the objects progress.
   * 
   * @return the progress
   */
  public Progress getProgress() {
    return progress;
  }

  /**
   * Generate the message only when needed.
   */
  @Override
  public String getMessage() {
    String message = super.getMessage();
    if (message == null) {
      message = progress.toString();
      super.setMessage(message);
    }
    return message;
  }
}
