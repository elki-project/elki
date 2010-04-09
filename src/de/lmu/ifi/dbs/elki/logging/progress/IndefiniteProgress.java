package de.lmu.ifi.dbs.elki.logging.progress;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Progress class without a fixed destination value.
 * 
 * @author Erich Schubert
 */
public class IndefiniteProgress extends AbstractProgress {
  /**
   * Store completion flag.
   */
  private boolean completed = false;
  
  /**
   * Constructor.
   * 
   * @param task Task name.
   */
  @Deprecated
  public IndefiniteProgress(String task) {
    super(task);
  }

  /**
   * Constructor with logging.
   * 
   * @param task Task name.
   * @param logger Logger to report to
   */
  public IndefiniteProgress(String task, Logging logger) {
    super(task);
    logger.progress(this);
  }

  /**
   * Serialize 'indefinite' progress.
   */
  @Override
  public StringBuffer appendToBuffer(StringBuffer buf) {
    buf.append(getTask());
    buf.append(": ");
    buf.append(getProcessed());
    return buf;
  }

  /**
   * Return whether the progress is complete
   * 
   * @return Completion status.
   */
  @Override
  public boolean isComplete() {
    return completed;
  }

  /**
   * Set the completion flag.
   */
  @Deprecated
  public void setCompleted() {
    this.completed = true;
  }

  /**
   * Set the completion flag and log it
   * 
   * @param logger Logger to report to.
   */
  public void setCompleted(Logging logger) {
    this.completed = true;
    logger.progress(this);
  }
}