package de.lmu.ifi.dbs.elki.database.relation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;

/**
 * An object representation from a database
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface Relation<O> extends DatabaseQuery, HierarchicalResult {
  /**
   * Get the associated database.
   * 
   * Note: in some situations, this might be {@code null}!
   * 
   * @return Database
   */
  public Database getDatabase();

  /**
   * Get the representation of an object.
   * 
   * @param id Object ID
   * @return object instance
   */
  public O get(DBID id);

  /**
   * Get the representation of an object.
   * 
   * @param iter Iterator pointing to the object
   * @return object instance
   */
  public O get(DBIDIter iter);

  /**
   * Set an object representation.
   * 
   * @param id Object ID
   * @param val Value
   */
  // TODO: remove / move to a writable API?
  public void set(DBID id, O val);

  /**
   * Delete an objects values.
   * 
   * @param id ID to delete
   */
  public void delete(DBID id);

  /**
   * Get the data type of this representation
   * 
   * @return Data type
   */
  public SimpleTypeInformation<O> getDataTypeInformation();

  /**
   * Get the IDs the query is defined for.
   * 
   * @return IDs this is defined for
   */
  public DBIDs getDBIDs();

  /**
   * Get an iterator access to the DBIDs.
   * 
   * To iterate over all IDs, use the following code fragment:
   * 
   * <pre>
   * {@code
   * for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
   *    DBID id = iter.getDBID();
   * }
   * </pre>
   * 
   * @return iterator for the DBIDs.
   */
  public DBIDIter iterDBIDs();

  /**
   * Get the number of DBIDs.
   * 
   * @return Size
   */
  public int size();
}