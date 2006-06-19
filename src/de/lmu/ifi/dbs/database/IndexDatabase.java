package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.index.Node;
import de.lmu.ifi.dbs.index.Entry;

/**
 * IndexDatabase is a database implementation which is supported by an index
 * structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class IndexDatabase<O extends DatabaseObject, N extends Node<E>, E extends Entry> extends AbstractDatabase<O> {

  /**
   * Calls the super method and afterwards deletes the specified object from
   * the underlying index structure.
   *
   * @see Database#delete(Integer)
   */
  public O delete(Integer id) {
    O object = super.delete(id);
    getIndex().delete(object);
    return object;
  }

  /**
   * Calls the super method and afterwards deletes the specified object from
   * the underlying index structure.
   *
   * @see Database#delete(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public void delete(O object) {
    super.delete(object);
    getIndex().delete(object);
  }

  /**
   * Returns the physical read access of this database.
   *
   * @return the physical read access of this database.
   */
  public long getPhysicalReadAccess() {
    return getIndex().getPhysicalReadAccess();
  }

   /**
   * Returns the physical write access of this database.
   *
   * @return the physical write access of this database.
   */
  public long getPhysicalWriteReadAccess() {
    return getIndex().getPhysicalWriteAccess();
  }

  /**
   * Returns the logical page access of this database.
   *
   * @return the logical page access of this database.
   */
  public long getLogicalPageAccess() {
    return getIndex().getLogicalPageAccess();
  }

  /**
   * Resets the page -access of this database.
   */
  public void resetPageAccess() {
    getIndex().resetPageAccess();
  }

  /**
   * Returns the underlying index structure.
   *
   * @return the underlying index structure
   */
  public abstract Index<O,N,E> getIndex();
}