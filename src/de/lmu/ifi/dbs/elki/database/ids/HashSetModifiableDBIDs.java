package de.lmu.ifi.dbs.elki.database.ids;

import java.util.Set;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
public interface HashSetModifiableDBIDs extends Set<DBID>, HashSetDBIDs, ModifiableDBIDs {
  /**
   * Retain all elements that also are in the second set.
   * 
   * @param set second set
   * @return true when modified
   */
  public boolean retainAll(DBIDs set);
}
