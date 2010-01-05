package de.lmu.ifi.dbs.elki.logging.progress;

/**
 * Progress class without a fixed destination value.
 * 
 * @author Erich Schubert
 */
public class IndefiniteProgress extends AbstractProgress {
  /**
   * Store completion flag.
   */
  private boolean completed;
  
  /**
   * Constructor.
   * 
   * @param task Task name.
   */
  public IndefiniteProgress(String task) {
    super(task);
    this.completed = false;
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
  public boolean complete() {
    return completed;
  }

  /**
   * Set the completion Flag.
   * 
   * @param completed boolean whether the progress is complete.
   */
  private void setCompleted(boolean completed) {
    this.completed = completed;
  }
  
  /**
   * Set the completion flag.
   */
  public void setCompleted() {
    this.setCompleted(true);
  }
}
