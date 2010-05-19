package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.lang.ref.WeakReference;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;

/**
 * Panel to handle result evaluation
 * 
 * @author Erich Schubert
 */
public class EvaluationTabPanel extends ParameterTabPanel implements Observer<Object> {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The data input configured
   */
  private EvaluationStep<DatabaseObject> evals = null;

  /**
   * Result we ran last onn
   */
  private WeakReference<? extends Object> basedOnResult = null;

  /**
   * Input step to run on.
   */
  private final InputTabPanel input;

  /**
   * Algorithm step to run on.
   */
  private final AlgorithmTabPanel algs;

  /**
   * Constructor. We depend on an input panel.
   * 
   * @param input Input panel to depend on.
   */
  public EvaluationTabPanel(InputTabPanel input, AlgorithmTabPanel algs) {
    super();
    this.input = input;
    this.algs = algs;
    input.addObserver(this);
    algs.addObserver(this);
  }

  @Override
  protected synchronized void configureStep(Parameterization config) {
    evals = new EvaluationStep<DatabaseObject>(config);
    if(config.getErrors().size() > 0) {
      evals = null;
    }
    basedOnResult = null;
  }

  @Override
  protected void executeStep() {
    if(input.canRun() && !input.isComplete()) {
      input.execute();
    }
    if(algs.canRun() && !algs.isComplete()) {
      algs.execute();
    }
    if(!input.isComplete() || !algs.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    // Get the database and run the algorithms
    Database<DatabaseObject> database = input.getInputStep().getDatabase();
    MultiResult result = algs.getAlgorithmStep().getResult();
    evals.runEvaluators(result, database, input.getInputStep().getNormalizationUndo(), input.getInputStep().getNormalization());
    // the result is cached by EvaluationStep, so we can just call getResult()
    // but not keep it
    evals.getResult();
    basedOnResult = new WeakReference<Object>(result);
  }

  /**
   * Get the evaluation step.
   * 
   * @return Evaluation step
   */
  public EvaluationStep<DatabaseObject> getEvaluationStep() {
    if(evals == null) {
      throw new AbortException("Evaluators not configured.");
    }
    return evals;
  }

  @Override
  protected String getStatus() {
    if(evals == null) {
      return STATUS_UNCONFIGURED;
    }
    if(!input.canRun() || !algs.canRun()) {
      return STATUS_CONFIGURED;
    }
    checkDependencies();
    if(input.isComplete() && algs.isComplete() && basedOnResult != null) {
      if(evals.getResult() == null) {
        return STATUS_FAILED;
      }
      else {
        return STATUS_COMPLETE;
      }
    }
    return STATUS_READY;
  }

  @Override
  public void update(Object o) {
    if(o == input || o == algs) {
      checkDependencies();
      updateStatus();
    }
  }

  /**
   * Test if the dependencies are still valid.
   */
  private void checkDependencies() {
    if(basedOnResult != null) {
      if(!input.isComplete() || !algs.isComplete() || basedOnResult.get() != algs.getAlgorithmStep().getResult()) {
        // We've become invalidated, notify.
        basedOnResult = null;
        observers.notifyObservers(this);
      }
    }
  }
}