package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mktab;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an MkCop-Tree node.
 * Additionally to an entry in an M-Tree an MkTabEntry holds a list of knn distances
 * for for parameters k <= k_max of the underlying data object or MkTab-Tree node.
 *
 * @author Elke Achtert
 */
interface MkTabEntry<D extends Distance<D>> extends MTreeEntry<D> {
  /**
   * Returns the list of knn distances of the entry.
   *
   * @return the list of  knn distances of the entry
   */
  public List<D> getKnnDistances();

  /**
   * Sets the knn distances of the entry.
   *
   * @param knnDistances the knn distances to be set
   */
  public void setKnnDistances(List<D> knnDistances);

  /**
   * Returns the knn distance of the entry for the specified parameter k.
   *
   * @param k the parameter k of the knn distance
   * @return the knn distance of the entry
   */
  public D getKnnDistance(int k);

  /**
   * Returns the parameter k_max.
   *
   * @return the parameter k_max
   */
  public int getK_max();

}
