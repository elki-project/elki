package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * An object representation from a database
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public interface DataQuery<O> extends DatabaseQuery {
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
  public void set(DBID id, O val);
  
  /**
   * Get the base class of data returned by this query.
   * 
   * @return Data base class
   */
  public Class<? super O> getDataClass();
}