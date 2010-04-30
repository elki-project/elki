package de.lmu.ifi.dbs.elki.database.ids;

import java.util.NavigableSet;

/**
 * Set-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
public interface TreeSetModifiableDBIDs extends ModifiableDBIDs, TreeSetDBIDs, NavigableSet<DBID> {
  // empty
}