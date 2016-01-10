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
 * Progress class with a moving target.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class MutableProgress extends AbstractProgress {
  /**
   * The overall number of items to process.
   */
  private int total;

  /**
   * Constructor with logging.
   * 
   * @param task Task name.
   * @param total Initial value of total.
   * @param logger Logger to report to
   */
  public MutableProgress(String task, int total, Logging logger) {
    super(task);
    this.total = total;
    logger.progress(this);
  }

  /**
   * Serialize 'indefinite' progress.
   */
  @Override
  public StringBuilder appendToBuffer(StringBuilder buf) {
    int percentage = (int) (getProcessed() * 100.0 / total);
    buf.append(getTask());
    buf.append(": ");
    buf.append(getProcessed());
    buf.append("/");
    buf.append(total);
    buf.append(" [");
    if (percentage < 100) {
      buf.append(' ');
    }
    if (percentage < 10) {
      buf.append(' ');
    }
    buf.append(percentage);
    buf.append("%]");
    return buf;
  }

  /**
   * Return whether the progress is complete
   * 
   * @return Completion status.
   */
  @Override
  public boolean isComplete() {
    return getProcessed() == total;
  }

  /**
   * Modify the total value.
   * 
   * @param total
   * @throws IllegalArgumentException
   */
  public void setTotal(int total) throws IllegalArgumentException {
    if (getProcessed() > total) {
      throw new IllegalArgumentException(getProcessed() + " exceeds total: " + total);
    }
    this.total = total;
  }

  /**
   * Get the current value of total.
   * 
   * @return total
   */
  public int getTotal() {
    return total;
  }
}
