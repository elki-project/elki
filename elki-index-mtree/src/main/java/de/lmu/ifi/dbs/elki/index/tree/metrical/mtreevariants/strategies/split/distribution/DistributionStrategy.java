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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.distribution;

import de.lmu.ifi.dbs.elki.index.tree.AbstractNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * M-tree entry distribution strategies.
 *
 * These are used by many split strategies to distribute the remaining entries.
 *
 * @author Erich Schubert
 * @since 0.7.5
 * 
 * @assoc - - - Assignments
 */
public interface DistributionStrategy {
  /**
   * Creates a balanced partition of the entries of the specified node.
   * 
   * @param node the node to be split
   * @param routing1 the entry number of the first routing object
   * @param dis1 Distances from first routing object
   * @param routing2 the entry number of the second routing object
   * @param dis2 Distances from second routing object
   * @param <E> entry type
   * @return an assignment that holds a balanced partition of the entries of the
   *         specified node
   */
  <E extends MTreeEntry> Assignments<E> distribute(AbstractNode<E> node, int routing1, double[] dis1, int routing2, double[] dis2);
}
