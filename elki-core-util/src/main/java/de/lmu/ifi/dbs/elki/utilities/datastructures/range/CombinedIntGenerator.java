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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.IntConsumer;

/**
 * Combine multiple ranges.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class CombinedIntGenerator implements IntGenerator {
  /**
   * Minimum.
   */
  private int min = Integer.MAX_VALUE;

  /**
   * Maximum value.
   */
  private int max = Integer.MIN_VALUE;

  /**
   * Generators.
   */
  private Collection<IntGenerator> generators;

  /**
   * Constructor.
   *
   * @param generators Data generators.
   */
  public CombinedIntGenerator(IntGenerator... generators) {
    this.generators = Arrays.asList(generators);
  }

  /**
   * Constructor.
   *
   * @param generators Data generators. Will <em>not</em> be copied. Modifying
   *        this afterwards can cause incorrect results, use with care.
   */
  public CombinedIntGenerator(Collection<IntGenerator> generators) {
    this.generators = generators;
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
    min = Integer.MAX_VALUE;
    max = Integer.MIN_VALUE;
    if(generators.isEmpty()) {
      return; // Keep invalid.
    }
    for(IntGenerator gen : generators) {
      final int mi = gen.getMin(), ma = gen.getMax();
      if(mi > ma) {
        continue;
      }
      min = mi < min ? mi : min;
      max = ma > max ? ma : max;
    }
  }

  @Override
  public void forEach(IntConsumer c) {
    for(IntGenerator gen : generators) {
      gen.forEach(c);
    }
  }

  @Override
  public StringBuilder serializeTo(StringBuilder buf) {
    boolean first = true;
    for(IntGenerator gen : generators) {
      if(gen.getMin() > gen.getMax()) {
        continue;
      }
      if(!first) {
        buf.append(',');
      }
      first = false;
      buf = gen.serializeTo(buf);
    }
    return buf;
  }

  @Override
  public String toString() {
    return serializeTo(new StringBuilder(1000)).toString();
  }
}
