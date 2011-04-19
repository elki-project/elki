package de.lmu.ifi.dbs.elki.database.relation;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;

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
   * @return iterator for the DBIDs.
   */
  public IterableIterator<DBID> iterDBIDs();

  /**
   * Get the number of DBIDs.
   * 
   * @return Size
   */
  public int size();
}