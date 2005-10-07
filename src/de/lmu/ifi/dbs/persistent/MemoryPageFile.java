package de.lmu.ifi.dbs.persistent;

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a PageFile that simulates I/O-access.<br>
 * Implemented as a Map with keys representing the ids of the saved pages.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MemoryPageFile<T extends Page> extends PageFile<T> {

  /**
   * Holds the pages.
   */
  private final Map<Integer, T> file;

  /**
   * Creates a new MemoryPageFile that is supported by a cache with the specified parameters.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Byte
   * @param cache     the class of the cache to be used
   */
  public MemoryPageFile(int pageSize, int cacheSize, Cache<T> cache) {
    super();
    initCache(pageSize, cacheSize, cache);
    this.file = new HashMap<Integer, T>();
  }

  /**
   * @see CachedFile#objectRemoved(Page)
   */
  public synchronized void objectRemoved(T page) {
    ioAccess++;
    file.put(page.getID(), page);
  }

  /**
   * Reads the page with the given id from this file.
   *
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  public synchronized T readPage(int pageID) {
    // try to get from cache
    T page = super.readPage(pageID);

    // get from file and put to cache
    if (page == null) {
      ioAccess++;
      page = file.get(pageID);
      if (page != null)
        cache.put(page);
    }

    return page;
  }

  /**
   * Deletes the node with the specified id from this file.
   *
   * @param pageID the id of the node to be deleted
   */
  public synchronized void deletePage(int pageID) {
    // put id to empty nodes and
    // delete from cache
    super.deletePage(pageID);

    // delete from file
    ioAccess++;
    file.remove(pageID);
  }

  /**
   * Clears this PageFile.
   */
  public void clear() {
    super.clear();
    file.clear();
  }
}
