package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for an underlying file of a cache.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface CachedFile {

  /**
   * This method is called by the cache if the <code>object</code> is not longer
   * stored in the cache and has to be written to disk.
   *
   * @param object the object which has to be written to disk
   */
  void write(Identifiable object);
}
