package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Database API with updates. Static databases allow for certain optimizations
 * that cannot be done in dynamic databases.
 * 
 * @author Erich Schubert
 */
public interface UpdatableDatabase extends Database {
  /**
   * Inserts the given object(s) and their associations into the database.
   * 
   * @param objpackages the objects to be inserted
   * @return the IDs assigned to the inserted objects
   * @throws UnableToComplyException if insertion is not possible
   */
  DBIDs insert(ObjectBundle objpackages) throws UnableToComplyException;

  /**
   * Removes and returns the specified objects with the given ids from the
   * database.
   * 
   * @param ids the ids of the object to be removed from the database
   * @return the objects that have been removed
   * @throws UnableToComplyException if deletion is not possible
   */
  ObjectBundle delete(DBIDs ids);
}