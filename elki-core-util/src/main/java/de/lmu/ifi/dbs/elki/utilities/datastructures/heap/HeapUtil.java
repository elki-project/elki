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
package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

/**
 * Next power of 2, for heaps.
 *
 * Usually, you should prefer the version in the
 * {@link de.lmu.ifi.dbs.elki.math.MathUtil} class. This copy exists to avoid
 * depending onto math from these data structures.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public final class HeapUtil {
  /**
   * Private constructor. Static methods only.
   */
  private HeapUtil() {
    // Do not use.
  }

  /**
   * Find the next power of 2.
   *
   * Classic bit operation, for signed 32-bit. Valid for positive integers only
   * (0 otherwise).
   *
   * @param x original integer
   * @return Next power of 2
   */
  public static int nextPow2Int(int x) {
    --x;
    x |= x >>> 1;
    x |= x >>> 2;
    x |= x >>> 4;
    x |= x >>> 8;
    x |= x >>> 16;
    return ++x;
  }
}
