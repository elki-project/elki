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
package elki.gui.multistep.panels;

import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.workflow.InputStep;

/**
 * Panel to handle data input.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class InputTabPanel extends ParameterTabPanel {
  /**
   * Serial version. 
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * The data input configured
   */
  private InputStep input = null;
  
  /**
   * Signal when an database input has been executed. 
   */
  private boolean executed = false;

  @Override
  protected synchronized void configureStep(Parameterization config)  {
    input = config.tryInstantiate(InputStep.class);
    if (config.getErrors().size() > 0) {
      input = null;
    }
    executed = false;
  }
  
  @Override
  protected void executeStep() {
    // the result is cached by InputStep, so we can just call getDatabase() and discard the returned value.
    input.getDatabase();
    executed = true;
  }

  /**
   * Get the input step object.
   * 
   * @return input step
   */
  public InputStep getInputStep() {
    if (input == null) {
      throw new AbortException("Data input not configured.");
    }
    return input;
  }

  @Override
  protected Status getStatus() {
    if (input == null) {
      return Status.STATUS_UNCONFIGURED;
    }
    if (executed) {
      if (input.getDatabase() == null) {
        return Status.STATUS_FAILED;
      }
      return Status.STATUS_COMPLETE;
    }
    return Status.STATUS_READY;
  }
}