package de.lmu.ifi.dbs.elki.index.tree;

/**
 * Directory Entry
 * 
 * @author Erich Schubert
 */
public interface DirectoryEntry extends Entry {
  /**
   * Get the page ID of this leaf entry.
   */
  public Integer getPageID();
  
  /**
   * Sets the id of the node or data object that is represented by this entry.
   * 
   * @param id the id to be set
   */
  //void setPageID(Integer id);
}