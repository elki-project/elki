/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree;

import java.util.Iterator;

import elki.persistent.Page;

/**
 * This interface defines the common requirements of nodes in an index
 * structure. A node has to extend the page interface for persistent storage and
 * has to provide an enumeration over its children.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @composed - - - Entry
 * 
 * @param <E> the type of Entry used in the index
 */
public interface Node<E> extends Page {
  /**
   * Returns an enumeration of the children paths of this node.
   * 
   * @param parentPath the path to this node
   * @return an enumeration of the children paths of this node
   */
  Iterator<IndexTreePath<E>> children(IndexTreePath<E> parentPath);

  /**
   * Returns the number of entries of this node.
   * 
   * @return the number of entries of this node
   */
  int getNumEntries();

  /**
   * Returns true if this node is a leaf node, false otherwise.
   * 
   * @return true if this node is a leaf node, false otherwise
   */
  boolean isLeaf();

  /**
   * Returns the entry at the specified index.
   * 
   * @param index the index of the entry to be returned
   * @return the entry at the specified index
   */
  E getEntry(int index);

  /**
   * Adds a new entry to this node's children and returns the index of
   * the entry in the children array. An IllegalStateException will be thrown
   * when inserting leaf entries into directory nodes or conversely.
   *
   * @param entry the entry to be added
   * @return the index of the entry in this node's children array
   * @throws IllegalStateException when inserting leaf entries into non-leaf
   *         nodes or conversely
   */
  int addEntry(E entry);
}
