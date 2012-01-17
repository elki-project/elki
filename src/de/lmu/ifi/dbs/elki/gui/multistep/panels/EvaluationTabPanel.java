package de.lmu.ifi.dbs.elki.gui.multistep.panels;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.ref.WeakReference;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
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
  private EvaluationStep evals = null;

  /**
   * Result we ran last onn
   */
  private WeakReference<?> basedOnResult = null;

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
    evals = config.tryInstantiate(EvaluationStep.class);
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
    Database database = input.getInputStep().getDatabase();
    HierarchicalResult result = algs.getAlgorithmStep().getResult();
    evals.runEvaluators(result, database);
    basedOnResult = new WeakReference<Object>(result);
  }

  /**
   * Get the evaluation step.
   * 
   * @return Evaluation step
   */
  public EvaluationStep getEvaluationStep() {
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
      //if(evals.getResult() == null) {
        //return STATUS_FAILED;
      //}
      //else {
        return STATUS_COMPLETE;
      //}
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