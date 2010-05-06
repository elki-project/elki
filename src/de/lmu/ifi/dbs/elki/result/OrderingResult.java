package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIterator;

/**
 * Interface for a result providing an object ordering.
 * 
 * @author Erich Schubert
 */
public interface OrderingResult extends Result {
  /**
   * Sort the given ids according to this ordering and return an iterator.
   * 
   * @param ids Collection of ids.
   * @return iterator for sorted array of ids
   */
  public IterableIterator<DBID> iter(DBIDs ids);
}
