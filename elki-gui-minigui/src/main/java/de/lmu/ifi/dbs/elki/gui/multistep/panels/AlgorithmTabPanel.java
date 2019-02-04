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
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * Panel to handle data processing
 *
 * @author Erich Schubert
 * @since 0.4.0
 */
public class AlgorithmTabPanel extends ParameterTabPanel {
  /**
   * Serial version.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The data input configured
   */
  private AlgorithmStep algorithms = null;

  /**
   * Database we ran last onn
   */
  private WeakReference<?> basedOnDatabase = null;

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
    input.addPanelListener(this);
  }

  @Override
  protected synchronized void configureStep(Parameterization config) {
    algorithms = config.tryInstantiate(AlgorithmStep.class);
    if (config.getErrors().size() > 0) {
      algorithms = null;
    }
    basedOnDatabase = null;
  }

  @Override
  protected void executeStep() {
    if (input.canRun() && !input.isComplete()) {
      input.execute();
    }
    if (!input.isComplete()) {
      throw new AbortException("Input data not available.");
    }
    // Get the database and run the algorithms
    Database database = input.getInputStep().getDatabase();
    algorithms.runAlgorithms(database);
    basedOnDatabase = new WeakReference<Object>(database);
  }

  @Override
  protected Status getStatus() {
    if (algorithms == null) {
      return Status.STATUS_UNCONFIGURED;
    }
    if (!input.canRun()) {
      return Status.STATUS_CONFIGURED;
    }
    checkDependencies();
    if (input.isComplete() && basedOnDatabase != null) {
      if (algorithms.getResult() == null) {
        return Status.STATUS_FAILED;
      } else {
        return Status.STATUS_COMPLETE;
      }
    }
    return Status.STATUS_READY;
  }

  /**
   * Get the algorithm step object.
   *
   * @return Algorithm step
   */
  public AlgorithmStep getAlgorithmStep() {
    if (algorithms == null) {
      throw new AbortException("Algorithms not configured.");
    }
    return algorithms;
  }

  @Override
  public void panelUpdated(ParameterTabPanel o) {
    if (o == input) {
      checkDependencies();
      updateStatus();
    }
  }

  /**
   * Test if the dependencies are still valid.
   */
  private void checkDependencies() {
    if (basedOnDatabase != null) {
      if (!input.isComplete() || basedOnDatabase.get() != input.getInputStep().getDatabase()) {
        // We've become invalidated, notify.
        basedOnDatabase = null;
        firePanelUpdated();
      }
    }
  }
}
