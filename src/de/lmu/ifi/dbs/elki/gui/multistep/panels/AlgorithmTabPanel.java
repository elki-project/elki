package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.lang.ref.WeakReference;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.designpattern.Observer;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Panel to handle data processing
 * 
 * @author Erich Schubert
 */
public class AlgorithmTabPanel extends ParameterTabPanel implements Observer<Object> {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The data input configured
   */
  private AlgorithmStep<DatabaseObject> algorithms = null;

  /**
   * Database we ran last onn
   */
  private WeakReference<? extends Object> basedOnDatabase = null;

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
    input.addObserver(this);
  }

  @Override
  protected synchronized void configureStep(Parameterization config) {
    algorithms = new AlgorithmStep<DatabaseObject>(config);
    if(config.getErrors().size() > 0) {
      algorithms = null;
    }
    basedOnDatabase = null;
  }

  @Override
  protected void executeStep() {
    if(input.canRun() && !input.isComplete()) {
      input.execute();
    }
    if(!input.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    // Get the database and run the algorithms
    Database<DatabaseObject> database = input.getInputStep().getDatabase();
    algorithms.runAlgorithms(database);
    // the result is cached by AlgorithmStep, so we can just call getResult()
    // but not keep it
    algorithms.getResult();
    basedOnDatabase = new WeakReference<Object>(database);
  }

  @Override
  protected String getStatus() {
    if(algorithms == null) {
      return STATUS_UNCONFIGURED;
    }
    if(!input.canRun()) {
      return STATUS_CONFIGURED;
    }
    checkDependencies();
    if(input.isComplete() && basedOnDatabase != null) {
      if(algorithms.getResult() == null) {
        return STATUS_FAILED;
      }
      else {
        return STATUS_COMPLETE;
      }
    }
    return STATUS_READY;
  }

  /**
   * Get the algorithm step object.
   * 
   * @return Algorithm step
   */
  public AlgorithmStep<DatabaseObject> getAlgorithmStep() {
    if(algorithms == null) {
      throw new AbortException("Algorithms not configured.");
    }
    return algorithms;
  }

  @Override
  public void update(Object o) {
    if(o == input) {
      checkDependencies();
      updateStatus();
    }
  }

  /**
   * Test if the dependencies are still valid.
   */
  private void checkDependencies() {
    if(basedOnDatabase != null) {
      if(!input.isComplete() || basedOnDatabase.get() != input.getInputStep().getDatabase()) {
        // We've become invalidated, notify.
        basedOnDatabase = null;
        observers.notifyObservers(this);
      }
    }
  }
}