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
 * Generate an exponential range.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ExponentialIntGenerator implements IntGenerator {
  /**
   * Start value.
   */
  private int start;

  /**
   * Step factor.
   */
  private int factor;

  /**
   * End value.
   */
  private int end;

  /**
   * Constructor.
   *
   * @param start Start value
   * @param factor Step factor, must be positive
   * @param stop Stop value (inclusive, if reachable)
   */
  public ExponentialIntGenerator(int start, int factor, int stop) {
    if(factor <= 0) {
      throw new IllegalStateException("Only positive factors are supported.");
    }
    if(start == 0) {
      throw new IllegalStateException("Starting value must not be 0.");
    }
    if(start > 0 ? stop < start : stop > start) {
      throw new IllegalStateException("Maximum must not be less than the minimum.");
    }
    this.start = start;
    this.factor = factor;
    this.end = stop;
  }

  @Override
  public int getMin() {
    return start > 0 ? start : end;
  }

  @Override
  public int getMax() {
    return start > 0 ? end : start;
  }

  @Override
  public void forEach(IntConsumer c) {
    if(start > 0) {
      for(int i = start; i <= end; i *= factor) {
        c.accept(i);
      }
    }
    else {
      for(int i = start; i >= end; i *= factor) {
        c.accept(i);
      }
    }
  }

  @Override
  public StringBuilder serializeTo(StringBuilder buf) {
    return buf.append(start).append(",*=").append(factor).append(',').append(end);
  }

  @Override
  public String toString() {
    return serializeTo(new StringBuilder(100)).toString();
  }
}
