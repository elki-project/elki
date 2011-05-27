package de.lmu.ifi.dbs.elki.persistent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.<br>
 * This cache has a fixed maximum number of objects (<code>cacheSize</code>). If
 * the cache is full and another object is added, the LRU (least recently used)
 * object is dropped.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.composedOf CachedFile
 * 
 * @param <P> Page type
 */
public class LRUCache<P extends Page<P>> extends AbstractPageFile<P> {
  /**
   * Our logger
   */
  private static final Logging logger = Logging.getLogger(LRUCache.class);

  /**
   * Cache size in bytes.
   */
  protected long cacheSizeBytes;

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
  protected PageFile<P> file;

  /**
   * Initializes this cache with the specified parameters.
   * 
   * @param cacheSizeBytes the maximum number of pages in this cache
   * @param file the underlying file of this cache, if a page is dropped it is
   *        written to the file
   */
  public LRUCache(long cacheSizeBytes, PageFile<P> file) {
    this.file = file;
    this.cacheSizeBytes = cacheSizeBytes;
  }

  /**
   * Retrieves a page from the cache. The retrieved page becomes the MRU (most
   * recently used) page.
   * 
   * @param pageID the id of the page to be returned
   * @return the page associated to the id or null if no value with this key
   *         exists in the cache
   */
  @Override
  public synchronized P readPage(int pageID) {
    readAccess++;
    P page = map.get(pageID);
    if(page != null) {
      if(logger.isDebuggingFine()) {
        logger.debugFine("Read from cache: " + pageID);
      }
    }
    else {
      if(logger.isDebuggingFine()) {
        logger.debugFine("Read from backing: " + pageID);
      }
      page = file.readPage(pageID);
      map.put(pageID, page);
    }
    return page;
  }

  @Override
  public synchronized void writePage(Integer pageID, P page) {
    writeAccess++;
    page.setDirty(true);
    map.put(pageID, page);
    if(logger.isDebuggingFine()) {
      logger.debugFine("Write to cache: " + pageID);
    }
  }

  @Override
  public void deletePage(int pageID) {
    writeAccess++;
    map.remove(pageID);
    file.deletePage(pageID);
  }

  /**
   * Write page through to disk.
   * 
   * @param page page
   */
  protected void expirePage(P page) {
    if(logger.isDebuggingFine()) {
      logger.debugFine("Write to backing:" + page.getPageID());
    }
    if (page.isDirty()) {
      file.writePage(page);
    }
  }

  @Override
  public Integer setPageID(P page) {
    Integer pageID = file.setPageID(page);
    return pageID;
  }

  @Override
  public int getNextPageID() {
    return file.getNextPageID();
  }

  @Override
  public void setNextPageID(int nextPageID) {
    file.setNextPageID(nextPageID);
  }

  @Override
  public int getPageSize() {
    return file.getPageSize();
  }

  @Override
  public boolean initialize(PageHeader header) {
    boolean created = file.initialize(header);
    // Compute the actual cache size.
    this.cacheSize = cacheSizeBytes / header.getPageSize();

    if(this.cacheSize <= 0) {
      throw new AbortException("Invalid cache size: " + cacheSizeBytes + " / " + header.getPageSize() + " = " + cacheSize);
    }

    if(logger.isDebugging()) {
      logger.debug("LRU cache size is " + cacheSize + " pages.");
    }

    float hashTableLoadFactor = 0.75f;
    int hashTableCapacity = (int) Math.ceil(cacheSize / hashTableLoadFactor) + 1;

    this.map = new LinkedHashMap<Integer, P>(hashTableCapacity, hashTableLoadFactor, true) {
      private static final long serialVersionUID = 1L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<Integer, P> eldest) {
        if(size() > LRUCache.this.cacheSize) {
          expirePage(eldest.getValue());
          return true;
        }
        return false;
      }
    };
    return created;
  }

  @Override
  public void close() {
    flush();
    file.close();
  }

  /**
   * Flushes this caches by writing any entry to the underlying file.
   */
  public void flush() {
    for(P object : map.values()) {
      expirePage(object);
    }
    map.clear();
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
  @Override
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
      file.writePage(page);
    }
  }

  @Override
  public PageFileStatistics getInnerStatistics() {
    return file;
  }
}
