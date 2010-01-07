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
  private boolean completed = false;
  
  /**
   * Constructor.
   * 
   * @param task Task name.
   */
  public IndefiniteProgress(String task) {
    super(task);
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
  public void setCompleted() {
    this.completed = true;
  }
}
