package de.lmu.ifi.dbs.elki.logging.progress;

/**
 * This progress class is used for multi-step processing.
 * 
 * @author Erich Schubert
 */
public class StepProgress extends FiniteProgress {
  String stepTitle = "";

  /**
   * Constructor.
   * 
   * @param total Total number of steps.
   */
  public StepProgress(int total) {
    super("Step #", total);
  }

  /** {@inheritDoc} */
  @Override
  public StringBuffer appendToBuffer(StringBuffer buf) {
    buf.append("Step #" + getProcessed() + "/" + getTotal() + ": " + getStepTitle());
    return buf;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return super.toString();
  }

  /**
   * Do a new step.
   * 
   * @param step Step number
   * @param stepTitle Step title
   */
  public void beginStep(int step, String stepTitle) {
    setProcessed(step);
    this.stepTitle = stepTitle;
  }

  /**
   * Mark the progress as completed.
   */
  public void setCompleted() {
    setProcessed(getTotal());
  }

  /**
   * @return the stepTitle
   */
  protected String getStepTitle() {
    return stepTitle;
  }
}