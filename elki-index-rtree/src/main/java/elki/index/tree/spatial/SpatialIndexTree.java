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
package elki.index.tree.spatial;

import java.util.List;

import elki.index.tree.IndexTree;
import elki.persistent.PageFile;

/**
 * Abstract super class for all spatial index tree classes.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @opt nodefillcolor LemonChiffon
 * @navhas - contains - SpatialNode
 *
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class SpatialIndexTree<N extends SpatialNode<N, E>, E extends SpatialEntry> extends IndexTree<N, E> {
  /**
   * Constructor.
   *
   * @param pagefile Page file
   */
  public SpatialIndexTree(PageFile<N> pagefile) {
    super(pagefile);
  }

  /**
   * Add a new leaf entry to the tree.
   *
   * @param leaf Leaf entry
   */
  public abstract void insertLeaf(E leaf);

  /**
   * Returns a list of entries pointing to the leaf entries of this spatial
   * index.
   *
   * @return a list of entries pointing to the leaf entries of this spatial
   *         index
   */
  public abstract List<E> getLeaves();
}