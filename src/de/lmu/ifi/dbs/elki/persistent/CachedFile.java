package de.lmu.ifi.dbs.elki.persistent;

/**
 * Defines the requirements for an underlying file of a cache that stores
 * objects implementing the Page interface.
 * 
 * @author Elke Achtert
 */
public interface CachedFile<P extends Page<P>> {

  /**
   * This method is called by the cache if the <code>page</code> is not longer
   * stored in the cache and has to be written to disk.
   * 
   * @param page the page which has to be written to disk
   */
  void objectRemoved(P page);
}
