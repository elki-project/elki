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
package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Relations that allow modification.
 * 
 * Important: Relations are now responsible for maintaining their indexes after
 * creation (i.e. notifying them of inserts and deletions).
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Data type.
 */
public interface ModifiableRelation<O> extends Relation<O> {
  /**
   * Set (or insert) an object representation.
   * 
   * @param id Object ID
   * @param val Value
   */
  void insert(DBIDRef id, O val);

  /**
   * Delete an objects values.
   * 
   * @param id ID to delete
   */
  void delete(DBIDRef id);
}
