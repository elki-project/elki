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
package de.lmu.ifi.dbs.elki.utilities.datastructures.unionfind;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Union-find implementations in ELKI, for DBID objects.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public interface UnionFind {
  /**
   * Join the components of elements p and q.
   *
   * @param p First element
   * @param q Second element
   * @return Component id.
   */
  int union(DBIDRef p, DBIDRef q);

  /**
   * Test if two components are connected.
   *
   * @param p First element
   * @param q Second element
   * @return {@code true} if they are in the same component.
   */
  boolean isConnected(DBIDRef p, DBIDRef q);

  /**
   * Collect all component root elements.
   *
   * @return Root elements
   */
  DBIDs getRoots();

  /**
   * Find the component ID of an element.
   *
   * @param p Element
   * @return Component id
   */
  int find(DBIDRef p);
}
