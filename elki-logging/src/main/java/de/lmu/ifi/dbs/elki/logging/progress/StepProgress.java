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
package de.lmu.ifi.dbs.elki.logging.progress;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * This progress class is used for multi-step processing.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class StepProgress extends FiniteProgress {
  /**
   * Title of the current step.
   */
  String stepTitle = "";

  /**
   * Constructor.
   * This constructor does not use a logger; initial logging will happen on the first beginStep call.
   * 
   * @param total Total number of steps.
   */
  public StepProgress(int total) {
    super("Step", total);
  }
  
  /**
   * Constructor.
   * This constructor does not use a logger; initial logging will happen on the first beginStep call.
   *
   * @param task Task title
   * @param total Total number of steps.
   */
  public StepProgress(String task, int total) {
    super(task, total);
  }
  
  // No constructor with auto logging - call beginStep() first

  @Override
  public StringBuilder appendToBuffer(StringBuilder buf) {
    buf.append(super.getTask());
    if (isComplete()) {
      buf.append(": complete.");
    } else {
      buf.append(" #").append(getProcessed()+1).append('/').append(getTotal());
      buf.append(": ").append(getStepTitle());
    }
    buf.append('\n');
    return buf;
  }

  /**
   * Do a new step and log it
   * 
   * @param step Step number
   * @param stepTitle Step title
   * @param logger Logger to report to.
   */
  public void beginStep(int step, String stepTitle, Logging logger) {
    setProcessed(step - 1);
    this.stepTitle = stepTitle;
    logger.progress(this);
  }

  /**
   * Mark the progress as completed and log it.
   *
   * @param logger Logger to report to.
   */
  public void setCompleted(Logging logger) {
    setProcessed(getTotal());
    logger.progress(this);
  }

  /**
   * @return the stepTitle
   */
  protected String getStepTitle() {
    return stepTitle;
  }
}