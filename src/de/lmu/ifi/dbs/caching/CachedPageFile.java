package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for an underlying file of a cache.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface CachedPageFile {

  /**
   * This method is called by the cache if the <code>page</code> is not longer 
   * stored in the cache and has to be written to disk.
   *
   * @param page the page which has to be written to disk
   */
  void write(Page page);
}
