package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.List;

/**
 * Defines the requirements for an index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Index<O extends MetricalObject> {

  /**
   * Inserts the specified object into this index.
   *
   * @param o the vector to be inserted
   */
  void insert(O o);

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  boolean delete(O o);

  /**
   * Returns the IO-Access of this index.
   *
   * @return the IO-Access of this index
   */
  long getIOAccess();

  /**
   * Resets the IO-Access of this index.
   */
  void resetIOAccess();
}
