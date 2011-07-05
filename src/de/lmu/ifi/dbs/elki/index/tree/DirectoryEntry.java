package de.lmu.ifi.dbs.elki.index.tree;

/**
 * Directory Entry
 * 
 * @author Erich Schubert
 */
public interface DirectoryEntry extends Entry {
  /**
   * Returns the id of the node or data object that is represented by this
   * entry.
   * 
   * @return the id of the node or data object that is represented by this entry
   */
  Integer getEntryID();

  /**
   * Get the page ID of this leaf entry.
   */
  public Integer getPageID();
}