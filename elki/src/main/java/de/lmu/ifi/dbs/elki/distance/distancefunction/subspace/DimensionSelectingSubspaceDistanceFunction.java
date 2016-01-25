package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;

/**
 * Interface for dimension selecting subspace distance functions.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <O> Object type
 */
public interface DimensionSelectingSubspaceDistanceFunction<O> extends DistanceFunction<O> {
  /**
   * Returns a bit set representing the selected dimensions.
   * 
   * @return a bit set representing the selected dimensions
   */
  public long[] getSelectedDimensions();

  /**
   * Sets the selected dimensions according to the set bits in the given BitSet.
   * 
   * @param dimensions a bit set designating the new selected dimensions
   */
  public void setSelectedDimensions(long[] dimensions);
}
