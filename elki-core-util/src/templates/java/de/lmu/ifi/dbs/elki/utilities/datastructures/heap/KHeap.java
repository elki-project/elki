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
package de.lmu.ifi.dbs.elki.utilities.datastructures.heap;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;

/**
 * Basic in-memory heap for ${type != "K" ? type : "Object"} values.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @has - - - UnsortedIter
${genu ? " *\n * @param "+genu+" Key type\n" : ""} */
public interface ${classname}${gend} {
  /**
   * Add a key-value pair to the heap
   *
   * @param key Key
   */
  void add(${type} key);

  /**
   * Add a key-value pair to the heap, except if the new element is larger than
   * the top, and we are at design size (overflow)
   *
   * @param key Key
   * @param max Maximum size of heap
   */
  void add(${type} key, int max);

  /**
   * Combined operation that removes the top element, and inserts a new element
   * instead.
   *
   * @param e New element to insert
   * @return Previous top element of the heap
   */
  ${type} replaceTopElement(${type} e);

  /**
   * Get the current top key
   *
   * @return Top key
   */
  ${type} peek();

  /**
   * Remove the first element
   *
   * @return Top element
   */
  ${type} poll();

  /**
   * Delete all elements from the heap.
   */
  void clear();

  /**
   * Query the size
   *
   * @return Size
   */
  int size();

  /**
   * Is the heap empty?
   *
   * @return {@code true} when the size is 0.
   */
  boolean isEmpty();

  /**
   * Get an unsorted iterator to inspect the heap.
   *
   * @return Iterator
   */
  UnsortedIter${genu} unsortedIter();

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   *
   * <pre>
   * {@code
   * for (${classname}.UnsortedIter${genu} iter = heap.unsortedIter(); iter.valid(); iter.next()) {
   *   doSomething(iter.get());
   * }
   * }
   * </pre>
   *
   * @author Erich Schubert
${genu ? " *\n   * @param "+genu+" Key type\n" : ""}   */
  interface UnsortedIter${gend} extends Iter {
    /**
     * Get the iterators current object.
     *
     * @return Current object
     */
    ${type} get();
  }
}
