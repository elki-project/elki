package de.lmu.ifi.dbs.caching;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of objects (<code>cacheSize</code>).
 * If the cache is full and another object is added, the LRU (least recently used)
 * object is dropped.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LRUCache implements Cache {
  /**
   * The maximum number of objects in this cache.
   */
  private final int cacheSize;

  /**
   * The map holding the objects of this cache.
   */
  private final LinkedHashMap<Integer, Identifiable> map;

  /**
   * The underlying file of this cache. If an object is dropped
   * it is written to the file.
   */
  private final CachedFile file;

  /**
   * Creates a new LRU cache.
   *
   * @param cacheSize the maximum number of pages in this cache
   * @param file      the underlying file of this cache, if a page is dropped
   *                  it is written to the file
   */
  public LRUCache(int cacheSize, CachedFile file) {
    this.cacheSize = cacheSize;
    this.file = file;

    float hashTableLoadFactor = 0.75f;
    int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;
    this.map = new LinkedHashMap(hashTableCapacity, hashTableLoadFactor, true) {
      protected boolean removeEldestEntry(Map.Entry eldest) {
        if (size() > LRUCache.this.cacheSize) {
          LRUCache.this.file.write((Identifiable) eldest.getValue());
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
   * @return the page associated to the id
   *         or null if no value with this key exists in the cache
   */
  public synchronized Identifiable get(int pageID) {
    return map.get(pageID);
  }

  /**
   * Adds a page to this cache.
   * If the cache is full, the LRU (least recently used) page is dropped and
   * written to file.
   *
   * @param page
   */
  public synchronized void put(Identifiable page) {
    map.put(page.getID(), page);
  }

  /**
   * Removes a page from this cache.
   *
   * @param pageID the number of the node to be removed.
   * @return the removed page
   */
  public synchronized Identifiable remove(int pageID) {
    return map.remove(pageID);
  }

  /**
   * Flushes this caches by writing any entry to the underlying file.
   */
  public void flush() {
    for (Identifiable object : map.values()) {
      file.write(object);
    }
    map.clear();
  }

 /**
  * Returns a string representation of this cache.
  * @return a string representation of this cache
  */
  public String toString() {
    return map.toString();
  }

  /**
   * Creates and returns a copy of this cache, the objects
   * stored in this cache are not cloned.
   *
   * @return a clone of this instance
   */
  public Cache copy() {
    LRUCache clone = new LRUCache(cacheSize, file);

    for (Identifiable object : map.values()) {
      clone.put(object);
    }

    return clone;
  }
}
