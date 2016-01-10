package de.lmu.ifi.dbs.elki.logging.progress;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Progress class without a fixed destination value.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class IndefiniteProgress extends AbstractProgress {
  /**
   * Store completion flag.
   */
  private boolean completed = false;
  
  /**
   * Constructor with logging.
   * 
   * @param task Task name.
   * @param logger Logger to report to
   */
  public IndefiniteProgress(String task, Logging logger) {
    super(task);
    logger.progress(this);
  }

  /**
   * Serialize 'indefinite' progress.
   */
  @Override
  public StringBuilder appendToBuffer(StringBuilder buf) {
    buf.append(getTask());
    buf.append(": ");
    buf.append(getProcessed());
    return buf;
  }

  /**
   * Return whether the progress is complete
   * 
   * @return Completion status.
   */
  @Override
  public boolean isComplete() {
    return completed;
  }

  /**
   * Set the completion flag and log it
   * 
   * @param logger Logger to report to.
   */
  public void setCompleted(Logging logger) {
    this.completed = true;
    logger.progress(this);
  }
}