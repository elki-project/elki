package experimentalcode.erich.newdblayer.ids;

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
  boolean addAll(DBIDs ids);
  
  /**
   * Remove DBIDs from collection.
   * 
   * @param ids IDs to remove.
   * @return {@code true} when modified
   */
  boolean removeAll(DBIDs ids);
  
  /**
   * Test for DBID containment.
   * 
   * @param ids IDs to test.
   * @return {@code true} when contained
   */
  boolean containsAll(DBIDs ids);
}
