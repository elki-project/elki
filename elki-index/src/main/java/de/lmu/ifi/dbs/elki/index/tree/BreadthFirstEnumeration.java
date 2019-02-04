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
package de.lmu.ifi.dbs.elki.index.tree;

import java.util.*;

/**
 * Breadth first enumeration over the nodes of an index structure.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @assoc - - - IndexTree
 * @has - - - IndexTreePath
 * 
 * @param <N> the type of Node used in the index
 * @param <E> the type of Entry used in the index
 */
public class BreadthFirstEnumeration<N extends Node<E>, E extends Entry> implements Iterator<IndexTreePath<E>> {
  /**
   * Represents an empty enumeration.
   */
  public final Iterator<IndexTreePath<E>> EMPTY_ENUMERATION = new Iterator<IndexTreePath<E>>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public IndexTreePath<E> next() {
      throw new NoSuchElementException("No more children");
    }
  };

  /**
   * The queue for the enumeration.
   */
  private Queue<Iterator<IndexTreePath<E>>> queue;

  /**
   * The index storing the nodes.
   */
  private IndexTree<N, E> index;

  /**
   * Creates a new breadth first enumeration with the specified node as root
   * node.
   * 
   * @param index the index tree storing the nodes
   * @param rootPath the root entry of the enumeration
   */
  public BreadthFirstEnumeration(final IndexTree<N, E> index, final IndexTreePath<E> rootPath) {
    super();
    this.queue = new LinkedList<>();
    this.index = index;

    Iterator<IndexTreePath<E>> root_enum = new Iterator<IndexTreePath<E>>() {
      boolean hasNext = true;

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      @Override
      public IndexTreePath<E> next() {
        hasNext = false;
        return rootPath;
      }
    };

    queue.offer(root_enum);
  }

  /**
   * Tests if this enumeration contains more elements.
   * 
   * @return <code>true</code> if and only if this enumeration object contains
   *         at least one more element to provide; <code>false</code> otherwise.
   */
  @Override
  public boolean hasNext() {
    return (!queue.isEmpty() && (queue.peek()).hasNext());
  }

  /**
   * Returns the next element of this enumeration if this enumeration object has
   * at least one more element to provide.
   * 
   * @return the next element of this enumeration.
   * @throws java.util.NoSuchElementException if no more elements exist.
   */
  @Override
  public IndexTreePath<E> next() {
    Iterator<IndexTreePath<E>> enumeration = queue.peek();
    IndexTreePath<E> nextPath = enumeration.next();

    Iterator<IndexTreePath<E>> children;
    if(nextPath.getEntry() instanceof LeafEntry) {
      children = EMPTY_ENUMERATION;
    }
    else {
      N node = index.getNode(nextPath.getEntry());
      children = node.children(nextPath);
    }

    if(!enumeration.hasNext()) {
      queue.remove();
    }
    if(children.hasNext()) {
      queue.offer(children);
    }
    return nextPath;
  }
}