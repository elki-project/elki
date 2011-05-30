package de.lmu.ifi.dbs.elki.database.ids;

import java.util.Set;

/**
 * Interface for DBIDs that support fast "set" operations, in particular
 * "contains" lookups.
 * 
 * @author Erich Schubert
 */
public interface SetDBIDs extends DBIDs, Set<DBID> {
  // empty marker interface
}