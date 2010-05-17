package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.gui.multistep.kddtask.AlgorithmStep;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Panel to handle data processing
 * 
 * @author Erich Schubert
 */
public class AlgorithmTabPanel extends ParameterTabPanel {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private AlgorithmStep<DatabaseObject> algorithms = null;

  /**
   * Signal when an database input has been executed. 
   */
  private boolean executed = false;

  /**
   * Input step to run on.
   */
  private final InputTabPanel input;
  
  /**
   * Constructor. We depend on an input panel.
   * 
   * @param input Input panel to depend on.
   */
  public AlgorithmTabPanel(InputTabPanel input) {
    super();
    this.input = input;
  }

  @Override
  protected synchronized void configureStep(Parameterization config)  {
    algorithms  = new AlgorithmStep<DatabaseObject>(config);
    if (config.getErrors().size() > 0) {
      algorithms = null;
    }
    executed = false;
  }
  
  @Override
  protected void executeStep() {
    if (!input.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    // Get the database and run the algorithms
    Database<DatabaseObject> database = input.getInputStep().getDatabase();
    algorithms.runAlgorithms(database);
    // the result is cached by AlgorithmStep, so we can just call getResult() but not keep it
    algorithms.getResult();
    executed = true;
  }

  @Override
  protected String getStatus() {
    if (algorithms == null) {
      return STATUS_UNCONFIGURED;
    }
    if (!input.isComplete()) {
      return "input data not available - run input first!";
    }
    if (executed) {
      if (algorithms.getResult() == null) {
        return "empty result";
      }
      return STATUS_COMPLETE;
    }
    return STATUS_CONFIGURED;
  }

  /**
   * Get the algorithm step object.
   * 
   * @return Algorithm step
   */
  public AlgorithmStep<DatabaseObject> getAlgorithmStep() {
    if (algorithms == null) {
      throw new AbortException("Algorithms not configured.");
    }
    return algorithms;
  }
}