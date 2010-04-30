package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Leaf entry
 * 
 * @author Erich Schubert
 */
public interface LeafEntry extends Entry {
  /**
   * Get the DBID of this leaf entry.
   */
  public DBID getDBID();
}
