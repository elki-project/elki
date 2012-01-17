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

import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
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
  private OutputStep outs = null;

  /**
   * Result we ran last on
   */
  private WeakReference<?> basedOnResult = null;

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
    HierarchicalResult result = evals.getEvaluationStep().getResult();
    outs.runResultHandlers(result);
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