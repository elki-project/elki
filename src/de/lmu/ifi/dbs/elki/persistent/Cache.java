package de.lmu.ifi.dbs.elki.persistent;

/**
 * Defines the requirements for a cache that stores objects implementing the
 * Page interface.
 * 
 * @author Elke Achtert
 * @param <P> Page type
 */
public interface Cache<P extends Page<P>> {

  /**
   * Initializes this cache with the specified parameters.
   * 
   * @param cacheSize the maximum number of pages in this cache
   * @param file the underlying file of this cache, if a page is dropped it is
   *        written to the file
   */
  void initialize(long cacheSize, CachedFile<P> file);

  /**
   * Retrieves a page from this cache.
   * 
   * @param id the id of the page to be returned.
   * @return the page associated to the id or null if no value with this key
   *         exists in the cache.
   */
  P get(int id);

  /**
   * Adds an object to this cache.
   * 
   * @param object the object to be added
   */
  void put(P object);

  /**
   * Removes an object from this cache.
   * 
   * @param id the id of the object to be removed
   * @return the removed object
   */
  P remove(int id);

  /**
   * Flushes this cache by writing any entry to the underlying file.
   */
  void flush();

  /**
   * Clears this cache.
   */
  void clear();

  /**
   * Sets the maximum size of this cache.
   * 
   * @param cacheSize the cache size to be set
   */
  void setCacheSize(int cacheSize);

  /**
   * Returns the number of page accesses.
   * 
   * @return the number of page accesses
   */
  long getPageAccess();

  /**
   * Resets the pages access of this cache.
   */
  void resetPageAccess();
}