/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.logging.progress;

import elki.logging.Logging;

/**
 * A progress object for a given overall number of items to process. The number
 * of already processed items at a point in time can be updated.
 * <p>
 * The main feature of this class is to provide a String representation of the
 * progress suitable as a message for printing to the command line interface.
 * 
 * @author Arthur Zimek
 * @since 0.1
 */
public class FiniteProgress extends AbstractProgress {
  /**
   * The overall number of items to process.
   */
  private final int total;

  /**
   * Holds the length of a String describing the total number.
   */
  // TODO: move this to a console logging related class instead?
  private final int totalLength;

  /**
   * Constructor.
   * 
   * @param task the name of the task
   * @param total the overall number of items to process
   */
  protected FiniteProgress(String task, int total) {
    super(task);
    this.total = total;
    this.totalLength = numDigits(total);
  }

  /**
   * Constructor with auto-reporting to logging.
   * 
   * @param task the name of the task
   * @param total the overall number of items to process
   * @param logger the logger to report to
   */
  public FiniteProgress(String task, int total, Logging logger) {
    super(task);
    this.total = total;
    this.totalLength = numDigits(total);
    logger.progress(this);
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @throws IllegalArgumentException if the given number is negative or exceeds
   *         the overall number of items to process
   */
  @Override
  protected void setProcessed(int processed) throws IllegalArgumentException {
    if(processed > total) {
      throw new IllegalArgumentException(processed + " exceeds total: " + total);
    }
    if(processed < 0) {
      throw new IllegalArgumentException("Negative number of processed: " + processed);
    }
    super.setProcessed(processed);
  }

  /**
   * Append a string representation of the progress to the given string buffer.
   * 
   * @param buf Buffer to serialize to
   * @return Buffer the data was serialized to.
   */
  @Override
  public StringBuilder appendToBuffer(StringBuilder buf) {
    final int p = getProcessed();
    buf.append(getTask()).append(": ");
    for(int i = 0, l = totalLength - numDigits(p); i < l; i++) {
      buf.append(' ');
    }
    int percentage = (int) (p * 100.0 / total);
    buf.append(p).append(" [") //
        .append(percentage < 10 ? "  " : percentage < 100 ? " " : "") //
        .append(percentage).append("%]");
    if(ratems > 0. && p < total) {
      int secs = (int) Math.round((total - p) / ratems / 1000. + .2);
      if(secs > 300) {
        buf.append(' ').append(secs / 60).append(" min remaining");
      }
      else {
        buf.append(' ').append(secs).append(" sec remaining");
      }
    }
    return buf;
  }

  /**
   * Length of a number.
   *
   * @param x Number
   * @return Number of digits
   */
  private int numDigits(int x) {
    return x == Integer.MIN_VALUE ? 10 : x < 0 ? 1 + numDigits(-x) : //
        x < 10000 ? (x < 100 ? (x < 10 ? 1 : 2) : (x < 1000 ? 3 : 4)) : //
            x < 100000000 ? (x < 1000000 ? (x < 100000 ? 5 : 6) : (x < 10000000 ? 7 : 8)) : //
                (x < 1000000000 ? 9 : 10); //
  }

  /**
   * Test whether the progress was completed.
   */
  @Override
  public boolean isComplete() {
    return getProcessed() == total;
  }

  /**
   * Get the final value for the progress.
   * 
   * @return final value
   */
  public int getTotal() {
    return total;
  }

  /**
   * Ensure that the progress was completed, to make progress bars disappear
   * 
   * @param logger Logger to report to.
   */
  public void ensureCompleted(Logging logger) {
    if(!isComplete()) {
      logger.warning("Progress had not completed automatically as expected: " + getProcessed() + "/" + total, new Throwable());
      setProcessed(getTotal());
      logger.progress(this);
    }
  }
}
