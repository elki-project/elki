package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.util.List;

/**
 * Interface defining the minimum requirements for all index classes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Index<O extends DatabaseObject> extends Parameterizable {

  /**
   * Returns the physical read access of this index.
   *
   * @return the physical read access of this index
   */
  public long getPhysicalReadAccess();

  /**
   * Returns the physical write access of this index.
   *
   * @return the physical write access of this index
   */
  public long getPhysicalWriteAccess();

  /**
   * Returns the logical page access of this index.
   *
   * @return the logical page access of this index
   */
  public long getLogicalPageAccess();

  /**
   * Resets the counters for page access.
   */
  public void resetPageAccess();

  /**
   * Closes this index.
   */
  public void close();

  /**
   * Inserts the specified object into this index.
   *
   * @param object the vector to be inserted
   */
  public void insert(O object);

  /**
   * Inserts the specified objects into this index. If a bulk load mode
   * is implemented, the objects are inserted in one bulk.
   *
   * @param objects the objects to be inserted
   */
  public void insert(List<O> objects);

  /**
   * Deletes the specified obect from this index.
   *
   * @param object the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  public boolean delete(O object);

  /**
   * Sets the database in the distance function of this index (if existing).
   *
   * @param database the database
   */
  public void setDatabase(Database<O> database);
}
