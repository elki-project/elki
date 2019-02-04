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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an MkCop-Tree node. Additionally to
 * an entry in an M-Tree conservative approximation of the knn distances is
 * provided.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - ApproximationLine
 */
public interface MkCoPEntry extends MTreeEntry {
  /**
   * Returns the conservative approximated knn distance of the entry.
   * 
   * @param k the parameter k of the knn distance
   * @return the conservative approximated knn distance of the entry
   */
  double approximateConservativeKnnDistance(int k);

  /**
   * Returns the conservative approximation line.
   * 
   * @return the conservative approximation line
   */
  ApproximationLine getConservativeKnnDistanceApproximation();

  /**
   * Sets the conservative approximation line
   * 
   * @param conservativeApproximation the conservative approximation line to be
   *        set
   */
  void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation);
}
