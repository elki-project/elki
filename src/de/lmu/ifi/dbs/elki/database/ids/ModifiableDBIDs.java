package de.lmu.ifi.dbs.elki.database.ids;

import java.util.Collection;

/**
 * Interface for a generic modifiable DBID collection.
 * 
 * @author Erich Schubert
 */
public interface ModifiableDBIDs extends DBIDs, Collection<DBID> {
  /**
   * Add DBIDs to collection.
   * 
   * @param ids IDs to add.
   * @return {@code true} when modified
   */
  boolean addDBIDs(DBIDs ids);
  
  /**
   * Remove DBIDs from collection.
   * 
   * @param ids IDs to remove.
   * @return {@code true} when modified
   */
  boolean removeDBIDs(DBIDs ids);
}
