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
import java.util.stream.IntStream;

/**
 * Generators of integer ranges.
 *
 * This is similar in spirit to the Java {@link IntStream}, but it allows the
 * stream to be processed multiple times, and allows accessing the minimum and
 * maximum. This is necessary to, e.g., precompute nearest neighbors for the
 * maximum k employed and similar.
 *
 * Duplicates values are not automatically removed, and the values are not
 * necessarily ordered.
 *
 * TODO: fully support the Java 8 {@link IntStream} API, in particular
 * spliterators for parallel processing.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public interface IntGenerator {
  /**
   * Minimum value of the stream.
   *
   * @return Minimum
   */
  int getMin();

  /**
   * Maximum value of the stream.
   *
   * @return Maximum
   */
  int getMax();

  /**
   * Process the entire series once.
   *
   * @param c Consumer
   */
  void forEach(IntConsumer c);

  /**
   * Produce a textual representation.
   *
   * @param buf Output buffer.
   * @return Buffer
   */
  StringBuilder serializeTo(StringBuilder buf);
}
