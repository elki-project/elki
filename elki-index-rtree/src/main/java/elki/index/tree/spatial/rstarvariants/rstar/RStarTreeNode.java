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
package elki.index.tree.spatial.rstarvariants.rstar;

import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Represents a node in an R*-Tree.
 * 
 * @author Elke Achtert
 * @since 0.1
 */
public class RStarTreeNode extends AbstractRStarTreeNode<RStarTreeNode, SpatialEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RStarTreeNode() {
    // empty constructor
  }

  /**
   * Creates a new RStarTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public RStarTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf);
  }
}