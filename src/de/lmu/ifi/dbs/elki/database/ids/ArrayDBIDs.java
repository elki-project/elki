package de.lmu.ifi.dbs.elki.database.ids;

import java.util.List;

/**
 * Interface for array based DBIDs.
 * 
 * @author Erich Schubert
 */
public interface ArrayDBIDs extends DBIDs, List<DBID> {
  /**
   * Get the i'th entry (starting at 0)
   * 
   * @param i Index
   * @return DBID of i'th entry.
   */
  // In List<DBID> which confuses the java compiler
  /* public DBID get(int i); */
}
