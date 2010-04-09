package de.lmu.ifi.dbs.elki.logging.progress;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * This progress class is used for multi-step processing.
 * 
 * @author Erich Schubert
 */
public class StepProgress extends FiniteProgress {
  /**
   * Title of the current step.
   */
  String stepTitle = "";

  /**
   * Constructor.
   * This constructor does not use a logger; initial logging will happen on the first beginStep call.
   * 
   * @param total Total number of steps.
   */
  @SuppressWarnings("deprecation")
  public StepProgress(int total) {
    super("Step", total);
  }
  
  // No constructor with auto logging - call beginStep() first

  /** {@inheritDoc} */
  @Override
  public StringBuffer appendToBuffer(StringBuffer buf) {
    buf.append(super.getTask() + "#" + getProcessed() + "/" + getTotal() + ": " + getStepTitle());
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
  @Deprecated
  public void beginStep(int step, String stepTitle) {
    setProcessed(step);
    this.stepTitle = stepTitle;
  }

  /**
   * Do a new step and log it
   * 
   * @param step Step number
   * @param stepTitle Step title
   * @param logger Logger to report to.
   */
  public void beginStep(int step, String stepTitle, Logging logger) {
    setProcessed(step);
    this.stepTitle = stepTitle;
    logger.progress(this);
  }

  /**
   * Mark the progress as completed.
   */
  @Deprecated
  public void setCompleted() {
    setProcessed(getTotal());
  }

  /**
   * Mark the progress as completed and log it.
   *
   * @param logger Logger to report to.
   */
  public void setCompleted(Logging logger) {
    setProcessed(getTotal());
    logger.progress(this);
  }

  /**
   * @return the stepTitle
   */
  protected String getStepTitle() {
    return stepTitle;
  }
}