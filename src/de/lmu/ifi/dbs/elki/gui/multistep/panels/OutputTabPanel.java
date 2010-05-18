package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.gui.multistep.kddtask.OutputStep;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Panel to handle result output / visualization
 * 
 * @author Erich Schubert
 */
public class OutputTabPanel extends ParameterTabPanel implements Observer<ParameterTabPanel> {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private OutputStep<DatabaseObject> outs = null;

  /**
   * Signal when an database input has been executed. 
   */
  private boolean executed = false;

  /**
   * Input step to run on.
   */
  private final InputTabPanel input;
  
  /**
   * Algorithm step to run on.
   */
  private final EvaluationTabPanel evals;
  
  /**
   * Constructor. We depend on an input panel.
   * 
   * @param input Input panel to depend on.
   */
  public OutputTabPanel(InputTabPanel input, EvaluationTabPanel evals) {
    super();
    this.input = input;
    this.evals = evals;
    input.addObserver(this);
    evals.addObserver(this);
  }

  @Override
  protected synchronized void configureStep(Parameterization config)  {
    outs  = new OutputStep<DatabaseObject>(config);
    if (config.getErrors().size() > 0) {
      outs = null;
    }
    executed = false;
  }
  
  @Override
  protected void executeStep() {
    if (input.canRun() && !input.isComplete()) {
      input.execute();
    }
    if (evals.canRun() && !evals.isComplete()) {
      evals.execute();
    }
    if (!input.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    if (!evals.isComplete()) {
      throw new AbortException("Evaluation failed.");
    }
    // Get the database and run the algorithms
    Database<DatabaseObject> database = input.getInputStep().getDatabase();
    MultiResult result = evals.getEvaluationStep().getResult();
    outs.runResultHandlers(result, database, input.getInputStep().getNormalizationUndo(), input.getInputStep().getNormalization());
    executed = true;
  }

  @Override
  protected String getStatus() {
    if (outs == null) {
      return STATUS_UNCONFIGURED;
    }
    if (!input.canRun() || !evals.canRun()) {
      return STATUS_CONFIGURED;
    }
    if (executed) {
      return STATUS_COMPLETE;
    }
    return STATUS_READY;
  }

  @Override
  public void update(ParameterTabPanel o) {
    if (o == input || o == evals) {
      updateStatus();
    }
  }
}