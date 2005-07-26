package de.lmu.ifi.dbs.caching;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of pages (<code>cacheSize</code>).
 * If the cache is full and another page is added, the LRU (least recently used)
 * page is dropped.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LRUCache implements Cache {
  /**
   * The maximum number of pages in this cache.
   */
  private final int cacheSize;

  /**
   * The map holding the pages of this cache.
   */
  private final LinkedHashMap<Integer, Page> map;

  /**
   * The underlying file of this cache. If a page is dropped
   * it is written to the file.
   */
  private final CachedPageFile file;

  /**
   * Creates a new LRU cache.
   *
   * @param cacheSize the maximum number of pages in this cache
   * @param file      the underlying file of this cache, if a page is dropped
   *                  it is written to the file
   */
  public LRUCache(int cacheSize, CachedPageFile file) {
    this.cacheSize = cacheSize;
    this.file = file;

    float hashTableLoadFactor = 0.75f;
    int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;
    this.map = new LinkedHashMap(hashTableCapacity, hashTableLoadFactor, true) {
      protected boolean removeEldestEntry(Map.Entry eldest) {
        if (size() > LRUCache.this.cacheSize) {
          LRUCache.this.file.write((Page) eldest.getValue());
          return true;
        }
        return false;
      }
    };
  }

  /**
   * Retrieves a page from the cache.
   * The retrieved page becomes the MRU (most recently used) page.
   *
   * @param pageID the id of the page to be returned
   * @return the page associated to the pageID
   *         or null if no value with this key exists in the cache
   */
  public synchronized Page get(int pageID) {
    return (Page) map.get(new Integer(pageID));
  }

  /**
   * Adds a page to this cache.
   * If the cache is full, the LRU (least recently used) page is dropped and
   * written to file.
   *
   * @param page
   */
  public synchronized void put(Page page) {
    map.put(new Integer(page.getPageID()), page);
  }

  /**
   * Removes a page from this cache.
   *
   * @param pageID the number of the node to be removed.
   * @return the removed page
   */
  public synchronized Page remove(int pageID) {
    return (Page) map.remove(new Integer(pageID));
  }

  /**
   * Clears the cache.
   */
  public void clear() {
    map.clear();
  }

 /**
  * Returns a string representation of this cache.
  * @return a string representation of this cache
  */
  public String toString() {
    return map.toString();
  }
}
