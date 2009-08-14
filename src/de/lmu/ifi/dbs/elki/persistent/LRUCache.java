package de.lmu.ifi.dbs.elki.persistent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of objects (<code>cacheSize</code>). If
 * the cache is full and another object is added, the LRU (least recently used)
 * object is dropped.
 * 
 * @author Elke Achtert
 * @param <P> Page type
 */
public class LRUCache<P extends Page<P>> implements Cache<P> {
  /**
   * The maximum number of objects in this cache.
   */
  protected long cacheSize;

  /**
   * The map holding the objects of this cache.
   */
  private LinkedHashMap<Integer, P> map;

  /**
   * The underlying file of this cache. If an object is dropped it is written to
   * the file.
   */
  protected CachedFile<P> file;

  /**
   * The number of read accesses
   */
  private long pageAccess;

  /**
   * Creates a new empty LRU cache.
   */
  public LRUCache() {
    // empty constructor
  }

  /**
   * Initializes this cache with the specified parameters.
   * 
   * @param cacheSize the maximum number of pages in this cache
   * @param file the underlying file of this cache, if a page is dropped it is
   *        written to the file
   */
  @SuppressWarnings("serial")
  public void initialize(long cacheSize, CachedFile<P> file) {
    this.file = file;
    assert(cacheSize <= Integer.MAX_VALUE);
    this.cacheSize = (int) cacheSize;
    this.pageAccess = 0;

    float hashTableLoadFactor = 0.75f;
    int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;

    this.map = new LinkedHashMap<Integer, P>(hashTableCapacity, hashTableLoadFactor, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Integer, P> eldest) {
        if(size() > LRUCache.this.cacheSize) {
          LRUCache.this.file.objectRemoved(eldest.getValue());
          return true;
        }
        return false;
      }
    };
  }

  /**
   * Retrieves a page from the cache. The retrieved page becomes the MRU (most
   * recently used) page.
   * 
   * @param pageID the id of the page to be returned
   * @return the page associated to the id or null if no value with this key
   *         exists in the cache
   */
  public synchronized P get(int pageID) {
    P page = map.get(pageID);
    if(page != null) {
      pageAccess++;
    }
    return page;
  }

  /**
   * Adds a page to this cache. If the cache is full, the LRU (least recently
   * used) page is dropped and written to file.
   * 
   * @param page the page to be added
   */
  public synchronized void put(P page) {
    pageAccess++;
    map.put(page.getID(), page);
  }

  /**
   * Removes a page from this cache.
   * 
   * @param pageID the number of the node to be removed.
   * @return the removed page
   */
  public synchronized P remove(int pageID) {
    P page = map.remove(pageID);
    if(page != null) {
      pageAccess++;
    }
    return page;
  }

  /**
   * Flushes this caches by writing any entry to the underlying file.
   */
  public void flush() {
    for(P object : map.values()) {
      file.objectRemoved(object);
    }
    map.clear();
  }

  /**
   * Returns the number of page accesses.
   * 
   * @return the number of page accesses
   */
  public long getPageAccess() {
    return pageAccess;
  }

  /**
   * Returns a string representation of this cache.
   * 
   * @return a string representation of this cache
   */
  @Override
  public String toString() {
    return map.toString();
  }

  /**
   * Clears this cache.
   */
  public void clear() {
    map.clear();
  }

  /**
   * Sets the maximum size of this cache.
   * 
   * @param cacheSize the cache size to be set
   */
  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;

    long toDelete = map.size() - this.cacheSize;
    if(toDelete <= 0) {
      return;
    }

    List<Integer> keys = new ArrayList<Integer>(map.keySet());
    Collections.reverse(keys);

    for(Integer id : keys) {
      P page = map.remove(id);
      pageAccess++;
      file.objectRemoved(page);
    }
  }

  /**
   * Resets the pages access of this cache.
   */
  public void resetPageAccess() {
    this.pageAccess = 0;
  }
}
