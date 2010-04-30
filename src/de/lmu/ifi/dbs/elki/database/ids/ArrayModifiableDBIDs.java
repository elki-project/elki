package de.lmu.ifi.dbs.elki.database.ids;

import java.util.List;

/**
 * Array-oriented implementation of a modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
public interface ArrayModifiableDBIDs extends ModifiableDBIDs, ArrayDBIDs, List<DBID> {
  // Empty interface
}