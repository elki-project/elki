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
 * Generate a static set of integers.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class StaticIntGenerator implements IntGenerator {
  /**
   * Minimum.
   */
  private int min = Integer.MAX_VALUE;

  /**
   * Maximum value.
   */
  private int max = Integer.MIN_VALUE;

  /**
   * Data to process.
   */
  private int[] values;

  /**
   * Constructor.
   *
   * @param values Values to generate. Will not be defensively copied, so do not
   *        modify afterwards anymore!
   */
  public StaticIntGenerator(int... values) {
    this.values = values;
  }

  @Override
  public int getMin() {
    if(min > max) {
      updateMinMax();
    }
    return min;
  }

  @Override
  public int getMax() {
    if(min > max) {
      updateMinMax();
    }
    return max;
  }

  /**
   * Compute the minimum and maximum.
   */
  private void updateMinMax() {
    if(values.length == 0) {
      return; // Keep invalid.
    }
    min = max = values[0];
    for(int i = 1; i < values.length; i++) {
      int v = values[i];
      min = min < v ? min : v;
      max = max > v ? max : v;
    }
  }

  @Override
  public void forEach(IntConsumer c) {
    for(int i : values) {
      c.accept(i);
    }
  }

  @Override
  public StringBuilder serializeTo(StringBuilder buf) {
    if(values.length == 0) {
      return buf;
    }
    buf.append(values[0]);
    if(values.length == 1) {
      return buf;
    }
    for(int i = 1; i < values.length; i++) {
      buf.append(',').append(values[i]);
    }
    return buf;
  }

  @Override
  public String toString() {
    return serializeTo(new StringBuilder(10 * values.length)).toString();
  }
}
