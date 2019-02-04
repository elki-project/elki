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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow;

import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Reinsertion strategy to resolve overflows in the RStarTree.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public interface OverflowTreatment {
  /**
   * Reinitialize the reinsertion treatment (for a new primary insertion).
   */
  void reinitialize();

  /**
   * Handle overflow in the given node.
   * 
   * @param <N> Node
   * @param <E> Entry
   * @param tree Tree
   * @param node Node
   * @param path Path
   * @return true when already handled (e.g. by reinserting)
   */
  <N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry> boolean handleOverflow(AbstractRStarTree<N, E, ?> tree, N node, IndexTreePath<E> path);
}