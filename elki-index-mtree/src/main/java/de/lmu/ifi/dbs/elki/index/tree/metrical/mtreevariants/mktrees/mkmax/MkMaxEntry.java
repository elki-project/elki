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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an
 * {@link de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax.MkMaxTreeNode}
 * . Additionally to an entry in an M-Tree an MkMaxEntry holds the k-nearest
 * neighbor distance of the underlying data object or MkMax-Tree node.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public interface MkMaxEntry extends MTreeEntry {
  /**
   * Returns the knn distance of the entry.
   * 
   * @return the knn distance of the entry
   */
  double getKnnDistance();

  /**
   * Sets the knn distance of the entry.
   * 
   * @param knnDistance the knn distance to be set
   */
  void setKnnDistance(double knnDistance);
}
