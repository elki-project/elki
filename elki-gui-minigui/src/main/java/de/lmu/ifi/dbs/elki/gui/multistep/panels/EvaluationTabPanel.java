/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.gui.multistep.panels;

import java.lang.ref.WeakReference;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.EvaluationStep;

/**
 * Panel to handle result evaluation
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class EvaluationTabPanel extends ParameterTabPanel {
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
    input.addPanelListener(this);
    algs.addPanelListener(this);
  }

  @Override
  protected synchronized void configureStep(Parameterization config) {
    evals = config.tryInstantiate(EvaluationStep.class);
    if (config.getErrors().size() > 0) {
      evals = null;
    }
    basedOnResult = null;
  }

  @Override
  protected void executeStep() {
    if (input.canRun() && !input.isComplete()) {
      input.execute();
    }
    if (algs.canRun() && !algs.isComplete()) {
      algs.execute();
    }
    if (!input.isComplete() || !algs.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    // Get the database and run the algorithms
    Database database = input.getInputStep().getDatabase();
    Result res = algs.getAlgorithmStep().getResult();
    evals.runEvaluators(database.getHierarchy(), database);
    basedOnResult = new WeakReference<Object>(res);
  }

  /**
   * Get the evaluation step.
   *
   * @return Evaluation step
   */
  public EvaluationStep getEvaluationStep() {
    if (evals == null) {
      throw new AbortException("Evaluators not configured.");
    }
    return evals;
  }

  @Override
  protected Status getStatus() {
    if (evals == null) {
      return Status.STATUS_UNCONFIGURED;
    }
    if (!input.canRun() || !algs.canRun()) {
      return Status.STATUS_CONFIGURED;
    }
    checkDependencies();
    if (input.isComplete() && algs.isComplete() && basedOnResult != null) {
      // if(evals.getResult() == null) {
      // return STATUS_FAILED;
      // }
      // else {
      return Status.STATUS_COMPLETE;
      // }
    }
    return Status.STATUS_READY;
  }

  @Override
  public void panelUpdated(ParameterTabPanel o) {
    if (o == input || o == algs) {
      checkDependencies();
      updateStatus();
    }
  }

  /**
   * Test if the dependencies are still valid.
   */
  private void checkDependencies() {
    if (basedOnResult != null) {
      if (!input.isComplete() || !algs.isComplete() || basedOnResult.get() != algs.getAlgorithmStep().getResult()) {
        // We've become invalidated, notify.
        basedOnResult = null;
        firePanelUpdated();
      }
    }
  }
}
