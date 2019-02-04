/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
 * @since 0.1
 * 
 * @assoc - - - PageFile
 * 
 * @param <P> Page type
 */
public class LRUCache<P extends Page> extends AbstractPageFile<P> {
  /**
   * Our class logger.
   */
  private static final Logging LOG = Logging.getLogger(LRUCache.class);

  /**
   * Cache size in bytes.
   */
  protected int cacheSizeBytes;

  /**
   * The maximum number of objects in this cache.
   */
  protected int cacheSize;

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
   * @param cacheSizeBytes the maximum number of bytes for this cache
   * @param file the underlying file of this cache, if a page is dropped it is
   *        written to the file
   */
  public LRUCache(int cacheSizeBytes, PageFile<P> file) {
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
    countRead();
    P page = map.get(pageID);
    if(page != null) {
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Read from cache: " + pageID);
      }
    }
    else {
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Read from backing: " + pageID);
      }
      page = file.readPage(pageID);
      map.put(pageID, page);
    }
    return page;
  }

  @Override
  public synchronized void writePage(int pageID, P page) {
    countWrite();
    page.setDirty(true);
    map.put(pageID, page);
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Write to cache: " + pageID);
    }
  }

  @Override
  public void deletePage(int pageID) {
    countWrite();
    map.remove(pageID);
    file.deletePage(pageID);
  }

  /**
   * Write page through to disk.
   * 
   * @param page page
   */
  protected void expirePage(P page) {
    if(LOG.isDebuggingFine()) {
      LOG.debugFine("Write to backing:" + page.getPageID());
    }
    if (page.isDirty()) {
      file.writePage(page);
    }
  }

  @Override
  public int setPageID(P page) {
    int pageID = file.setPageID(page);
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

    if(LOG.isDebugging()) {
      LOG.debug("LRU cache size is " + cacheSize + " pages.");
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

    List<Integer> keys = new ArrayList<>(map.keySet());
    Collections.reverse(keys);

    for(Integer id : keys) {
      P page = map.remove(id);
      file.writePage(page);
    }
  }

  @Override
  public void logStatistics() {
    super.logStatistics();
    file.logStatistics();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
