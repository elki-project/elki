package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Panel to handle data input.
 * 
 * @author Erich Schubert
 */
public class InputTabPanel extends ParameterTabPanel {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private InputStep input = null;
  
  /**
   * Signal when an database input has been executed. 
   */
  private boolean executed = false;

  @Override
  protected synchronized void configureStep(Parameterization config)  {
    input = config.tryInstantiate(InputStep.class);
    if (config.getErrors().size() > 0) {
      input = null;
    }
    executed = false;
  }
  
  @Override
  protected void executeStep() {
    // the result is cached by InputStep, so we can just call getDatabase() and discard the returned value.
    input.getDatabase();
    executed = true;
  }

  /**
   * Get the input step object.
   * 
   * @return input step
   */
  public InputStep getInputStep() {
    if (input == null) {
      throw new AbortException("Data input not configured.");
    }
    return input;
  }

  @Override
  protected String getStatus() {
    if (input == null) {
      return STATUS_UNCONFIGURED;
    }
    if (executed) {
      if (input.getDatabase() == null) {
        return "No database returned?";
      }
      return STATUS_COMPLETE;
    }
    return STATUS_READY;
  }
}