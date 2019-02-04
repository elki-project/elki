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
package de.lmu.ifi.dbs.elki.data.uncertain;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Interface for discrete uncertain objects, that are represented by a finite
 * (possibly weighted) number of samples.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface DiscreteUncertainObject extends UncertainObject {
  /**
   * Get the number of samples available (or -1 for infinite).
   *
   * @return Number of samples, negative indicates infinite.
   */
  int getNumberSamples();

  /**
   * Get the i'th vector.
   *
   * @param i Index
   * @return Vector
   */
  DoubleVector getSample(int i);

  /**
   * Get the weight of the i'th vector.
   *
   * @param i Index
   * @return Weight
   */
  double getWeight(int i);
}
