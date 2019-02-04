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
package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

/**
 * Interface for dimension selecting subspace distance functions.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <O> Object type
 */
public interface DimensionSelectingSubspaceDistanceFunction<O> extends DistanceFunction<O> {
  /**
   * Returns a bit set representing the selected dimensions.
   * <p>
   * Warning: no defensive copy is performed.
   * 
   * @return a bit set representing the selected dimensions
   */
  long[] getSelectedDimensions();

  /**
   * Sets the selected dimensions according to the set bits in the given BitSet.
   * <p>
   * Warning: no defensive copy is performed.
   * 
   * @param dimensions a bit set designating the new selected dimensions
   */
  void setSelectedDimensions(long[] dimensions);
}
