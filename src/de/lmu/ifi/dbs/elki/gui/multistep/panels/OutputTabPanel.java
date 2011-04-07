package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.lang.ref.WeakReference;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Panel to handle result output / visualization
 * 
 * @author Erich Schubert
 */
public class OutputTabPanel extends ParameterTabPanel implements Observer<Object> {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private OutputStep<DatabaseObject> outs = null;

  /**
   * Result we ran last on
   */
  private WeakReference<? extends Object> basedOnResult = null;

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

  @SuppressWarnings("unchecked")
  @Override
  protected synchronized void configureStep(Parameterization config)  {
    outs = config.tryInstantiate(OutputStep.class);
    if (config.getErrors().size() > 0) {
      outs = null;
    }
    basedOnResult = null;
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
    Result result = evals.getEvaluationStep().getResult();
    outs.runResultHandlers(result, database, input.getInputStep().getNormalizationUndo(), input.getInputStep().getNormalization());
    basedOnResult = new WeakReference<Object>(result);
  }

  @Override
  protected String getStatus() {
    if (outs == null) {
      return STATUS_UNCONFIGURED;
    }
    if (!input.canRun() || !evals.canRun()) {
      return STATUS_CONFIGURED;
    }
    checkDependencies();
    if (input.isComplete() && evals.isComplete() && basedOnResult != null) {
      // TODO: is there a FAILED state here, too?
      return STATUS_COMPLETE;
    }
    return STATUS_READY;
  }

  @Override
  public void update(Object o) {
    if (o == input || o == evals) {
      checkDependencies();
      updateStatus();
    }
  }
  
  /**
   * Test if the dependencies are still valid.
   */
  private void checkDependencies() {
    if(basedOnResult != null) {
      if(!input.isComplete() || !evals.isComplete() || basedOnResult.get() != evals.getEvaluationStep().getResult()) {
        // We've become invalidated, notify.
        basedOnResult = null;
        observers.notifyObservers(this);
      }
    }
  }
}