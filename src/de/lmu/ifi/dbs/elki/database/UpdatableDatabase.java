package de.lmu.ifi.dbs.elki.database;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;

/**
 * Database API with updates. Static databases allow for certain optimizations
 * that cannot be done in dynamic databases.
 * 
 * @author Erich Schubert
 */
public interface UpdatableDatabase extends Database {
  /**
   * Inserts the given objects and their associations into the database.
   * 
   * @param objpackages the objects to be inserted
   * @return the IDs assigned to the inserted objects
   * @throws UnableToComplyException if insertion is not possible
   */
  DBIDs insert(MultipleObjectsBundle objpackages) throws UnableToComplyException;

  /**
   * Inserts the given object package into the database.
   * 
   * @param objpackage the packaged object
   * @return the ID assigned to the inserted object
   * @throws UnableToComplyException if insertion is not possible
   */
  DBID insert(SingleObjectBundle objpackage) throws UnableToComplyException;

  /**
   * Removes and returns the object with the given id from the database.
   * 
   * @param id the id of an object to be removed from the database
   * @return the object that has been removed
   * @throws UnableToComplyException if deletion is not possible
   */
  SingleObjectBundle delete(DBID id);

  /**
   * Removes and returns the specified objects with the given ids from the
   * database.
   * 
   * @param ids the ids of the object to be removed from the database
   * @return the objects that have been removed
   * @throws UnableToComplyException if deletion is not possible
   */
  List<SingleObjectBundle> delete(DBIDs ids);
}