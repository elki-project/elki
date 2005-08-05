package de.lmu.ifi.dbs.caching;

/**
 * Defines the requirements for a cache that stores objects implementing the Identifiable interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Cache {

  /**
   * Retrieves a page from this cache.
   *
   * @param id the id of the page to be returned.
   * @return the page associated to the id or null if no value with this
   *         key exists in the cache.
   */
  public Identifiable get(int id);

  /**
   * Adds an object to this cache.
   *
   * @param object the object to be added
   */
  public void put(Identifiable object);

  /**
   * Removes an object from this cache.
   *
   * @param id the id of the object to be removed
   * @return the removed object
   */
  public Identifiable remove(int id);

  /**
   * Flushes this caches by writing any entry to the underlying file.
   */
  public void flush();

  /**
   * Creates and returns a copy of this cache, the objects
   * stored in this cache are not copied.
   *
   * @return a copy of this cache
   */
  public Cache copy();


}
