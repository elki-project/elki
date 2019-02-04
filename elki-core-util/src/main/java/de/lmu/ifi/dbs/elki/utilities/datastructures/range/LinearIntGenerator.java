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
package de.lmu.ifi.dbs.elki.utilities.datastructures.range;

import java.util.function.IntConsumer;

/**
 * Generate a linear range.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class LinearIntGenerator implements IntGenerator {
  /**
   * Start value.
   */
  private int start;

  /**
   * Step size.
   */
  private int step;

  /**
   * Stop value.
   */
  private int stop;

  /**
   * Constructor.
   *
   * @param start Start value
   * @param step Step size
   * @param stop Stop value (inclusive, if reachable by step size)
   */
  public LinearIntGenerator(int start, int step, int stop) {
    assert (start <= stop);
    if(step == 0 && start != stop) {
      throw new IllegalStateException("Step size cannot be zero.");
    }
    if((step > 0 && stop < start) || (step < 0 && stop > start)) {
      throw new IllegalStateException("Maximum must not be less than the minimum.");
    }
    this.start = start;
    this.step = step;
    this.stop = stop;
  }

  @Override
  public int getMin() {
    return start <= stop ? start : stop;
  }

  @Override
  public int getMax() {
    return start <= stop ? stop : start;
  }

  @Override
  public void forEach(IntConsumer c) {
    if(start <= stop) {
      for(int i = start; i <= stop; i += step) {
        c.accept(i);
      }
    }
    else {
      assert (step < 0);
      for(int i = start; i >= stop; i += step) {
        c.accept(i);
      }
    }
  }

  @Override
  public StringBuilder serializeTo(StringBuilder buf) {
    return buf.append(start).append(",+=").append(step).append(',').append(stop);
  }


  @Override
  public String toString() {
    return serializeTo(new StringBuilder(100)).toString();
  }
}
