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
 * Basic in-memory heap interface, for ${key.type != "K" ? key.type : "Object"} keys and ${val.type != "V" ? val.type : "Object"} values.
 *
 * This class is generated from a template.
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @has - - - UnsortedIter
${key.gend || val.gend ? " *\n" : ""}${key.gend ? " * @param "+key.genu+" Key type\n" : ""}${val.gend ? " * @param "+val.genu+" Value type\n" : ""} */
public interface ${classname}${key.gend}${val.gend} {
  /**
   * Add a key-value pair to the heap
   *
   * @param key Key
   * @param val Value
   */
  void add(${key.type} key, ${val.type} val);

  /**
   * Add a key-value pair to the heap if it improves the top.
   *
   * @param key Key
   * @param val Value
   * @param k Desired maximum size
   */
  void add(${key.type} key, ${val.type} val, int k);

  /**
   * Combined operation that removes the top element, and inserts a new element instead.
   *
   * @param key Key of new element
   * @param val Value of new element
   */
  void replaceTopElement(${key.type} key, ${val.type} val);

  /**
   * Get the current top key.
   *
   * @return Top key
   */
  ${key.type} peekKey();

  /**
   * Get the current top value.
   *
   * @return Value
   */
  ${val.type} peekValue();

  /**
   * Contains operation for a key (slow: with a linear scan).
   *
   * @param q Key
   * @return {@code true} if the key is contained in the heap.
   */
  boolean containsKey(${key.type} q);

  /**
   * Contains operation for a value (slow: with a linear scan).
   *
   * @param q Value
   * @return {@code true} if the value is contained in the heap.
   */
  boolean containsValue(${val.type} q);
  
  /**
   * Remove the first element.
   */
  void poll();

  /**
   * Clear the heap contents.
   */
  void clear();

  /**
   * Query the size.
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
  UnsortedIter${key.genu}${val.genu} unsortedIter();

  /**
   * Unsorted iterator - in heap order. Does not poll the heap.
   *
   * @author Erich Schubert
${key.gend || val.gend ? "   *\n" : ""}${key.gend ? "   * @param "+key.genu+" Key type\n" : ""}${val.gend ? "   * @param "+val.genu+" Value type\n" : ""}   */
  interface UnsortedIter${key.gend}${val.gend} extends Iter {
    /**
     * Get the current key.
     *
     * @return Current key
     */
    ${key.type} getKey();

    /**
     * Get the current value.
     *
     * @return Current value
     */
    ${val.type} getValue();
  }
}
