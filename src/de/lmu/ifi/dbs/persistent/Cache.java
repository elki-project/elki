package de.lmu.ifi.dbs.persistent;

/**
 * Defines the requirements for a cache that stores objects implementing the Page interface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface Cache<T extends Page> {

  /**
   * Initializes this cache with the specified parameters.
   *
   * @param cacheSize the maximum number of pages in this cache
   * @param file      the underlying file of this cache, if a page is dropped
   *                  it is written to the file
   */
  void initialize(int cacheSize, CachedFile<T> file);

  /**
   * Retrieves a page from this cache.
   *
   * @param id the id of the page to be returned.
   * @return the page associated to the id or null if no value with this
   *         key exists in the cache.
   */
  T get(int id);

  /**
   * Adds an object to this cache.
   *
   * @param object the object to be added
   */
  void put(T object);

  /**
   * Removes an object from this cache.
   *
   * @param id the id of the object to be removed
   * @return the removed object
   */
  T remove(int id);

  /**
   * Flushes this cache by writing any entry to the underlying file.
   */
  void flush();

  /**
   * Clears this cache.
   */
  void clear();
}
