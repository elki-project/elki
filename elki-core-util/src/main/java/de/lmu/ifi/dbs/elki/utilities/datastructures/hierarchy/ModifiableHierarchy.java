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
package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;

/**
 * Modifiable Hierarchy.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Object type
 */
public interface ModifiableHierarchy<O> extends Hierarchy<O> {
  /**
   * Add a parent-child relationship.
   *
   * @param parent Parent
   * @param child Child
   * @return {@code true} if changed
   */
  boolean add(O parent, O child);

  /**
   * Add an entry (initializes data structures).
   *
   * @param entry Entry
   * @return {@code true} if changed
   */
  boolean add(O entry);

  /**
   * Remove a parent-child relationship.
   *
   * @param parent Parent
   * @param child Child
   * @return {@code true} if changed
   */
  boolean remove(O parent, O child);

  /**
   * Remove an entry and all its parent-child relationships.
   *
   * @param entry Entry
   * @return {@code true} if changed
   */
  boolean remove(O entry);

  /**
   * Remove an entry and it's whole subtree (unless the elements are reachable
   * by a different path!)
   *
   * @param entry Entry
   * @return {@code true} if changed
   */
  boolean removeSubtree(O entry);
}
