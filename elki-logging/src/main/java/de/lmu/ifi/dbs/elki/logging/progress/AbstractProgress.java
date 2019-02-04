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

import java.util.concurrent.atomic.AtomicInteger;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for FiniteProgress objects.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public abstract class AbstractProgress implements Progress {
  /**
   * The number of items already processed at a time being.
   * 
   * We use AtomicInteger to allow threaded use without synchronization.
   */
  private AtomicInteger processed = new AtomicInteger(0);

  /**
   * The task name.
   */
  private String task;

  /**
   * For logging rate control.
   */
  private long lastLogged = 0;

  /**
   * Last logged value.
   */
  private int lastValue = 0;

  /**
   * Last rate.
   */
  protected double ratems = Double.NaN;

  /**
   * Default constructor.
   * 
   * @param task Task name.
   */
  public AbstractProgress(String task) {
    super();
    this.task = task;
  }

  /**
   * Provides the name of the task.
   * 
   * @return the name of the task
   */
  public String getTask() {
    return task;
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @throws IllegalArgumentException if an invalid value was passed.
   */
  protected void setProcessed(int processed) throws IllegalArgumentException {
    this.processed.set(processed);
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @param logger Logger to report to
   * @throws IllegalArgumentException if an invalid value was passed.
   */
  public void setProcessed(int processed, Logging logger) throws IllegalArgumentException {
    setProcessed(processed);
    if(testLoggingRate(processed)) {
      logger.progress(this);
    }
  }

  /**
   * Get the number of items already processed at a time being.
   * 
   * @return number of processed items
   */
  public int getProcessed() {
    return processed.get();
  }

  /**
   * Serialize a description into a String buffer.
   * 
   * @param buf Buffer to serialize to
   * @return Buffer the data was serialized to.
   */
  @Override
  public abstract StringBuilder appendToBuffer(StringBuilder buf);

  /**
   * Returns a String representation of the progress suitable as a message for
   * printing to the command line interface.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return appendToBuffer(new StringBuilder(200)).toString();
  }

  /**
   * Increment the processed counter.
   * 
   * @param logger Logger to report to.
   */
  public void incrementProcessed(Logging logger) {
    if(testLoggingRate(this.processed.incrementAndGet())) {
      logger.progress(this);
    }
  }

  /**
   * Logging rate control.
   *
   * @param processed Counter
   * @return true when logging is sensible
   */
  protected boolean testLoggingRate(int processed) {
    final long now = System.currentTimeMillis();
    if(processed > 10 && now - lastLogged < 5E2) {
      return isComplete();
    }
    synchronized(this) {
      final long age = now - lastLogged;
      if (age < 5E2) { // Probably another thread.
        return isComplete();
      }
      if(lastValue > 0) {
        int increment = processed - lastValue;
        double newrate = increment / (double) age;
        ratems = ratems != ratems ? newrate : (.95 * ratems + .05 * newrate);
      }
      lastValue = processed;
      lastLogged = now;
    }
    return true;
  }
}
