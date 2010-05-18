package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.gui.multistep.kddtask.EvaluationStep;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Panel to handle result evaluation
 * 
 * @author Erich Schubert
 */
public class EvaluationTabPanel extends ParameterTabPanel implements Observer<ParameterTabPanel> {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private EvaluationStep<DatabaseObject> evals = null;

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
  protected synchronized void configureStep(Parameterization config)  {
    evals  = new EvaluationStep<DatabaseObject>(config);
    if (config.getErrors().size() > 0) {
      evals = null;
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
    MultiResult result = algs.getAlgorithmStep().getResult();
    evals.runEvaluators(result, database, input.getInputStep().getNormalizationUndo(), input.getInputStep().getNormalization());
    // the result is cached by EvaluationStep, so we can just call getResult() but not keep it
    evals.getResult();
    executed = true;
  }

  /**
   * Get the evaluation step.
   * 
   * @return Evaluation step
   */
  public EvaluationStep<DatabaseObject> getEvaluationStep() {
    if (evals == null) {
      throw new AbortException("Evaluators not configured.");
    }
    return evals;
  }

  @Override
  protected String getStatus() {
    if (evals == null) {
      return STATUS_UNCONFIGURED;
    }
    if (!input.isComplete()) {
      return "input data not available - run input first!";
    }
    if (!algs.isComplete()) {
      return "algorithm output not available - run algorithm first!";
    }
    if (executed) {
      if (evals.getResult() == null) {
        return "empty result";
      }
      return STATUS_COMPLETE;
    }
    return STATUS_CONFIGURED;
  }

  @Override
  public void update(ParameterTabPanel o) {
    if (o == input || o == algs) {
      updateStatus();
    }
  }
}