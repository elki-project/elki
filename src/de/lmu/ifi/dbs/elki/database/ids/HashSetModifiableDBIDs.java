package de.lmu.ifi.dbs.elki.database.ids;

import java.util.Set;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
public interface HashSetModifiableDBIDs extends Set<DBID>, HashSetDBIDs, ModifiableDBIDs {
  // Empty interface
}
