package de.lmu.ifi.dbs.elki.persistent;

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a PageFile that simulates I/O-access.<br>
 * Implemented as a Map with keys representing the ids of the saved pages.
 * 
 * @author Elke Achtert
 * @param <P> Page type
 */
public class MemoryPageFile<P extends Page<P>> extends PageFile<P> {

  /**
   * Holds the pages.
   */
  private final Map<Integer, P> file;

  /**
   * Creates a new MemoryPageFile that is supported by a cache with the
   * specified parameters.
   * 
   * @param pageSize the size of a page in Bytes
   * @param cacheSize the size of the cache in Byte
   * @param cache the class of the cache to be used
   */
  public MemoryPageFile(int pageSize, int cacheSize, Cache<P> cache) {
    super();
    initCache(pageSize, cacheSize, cache);
    this.file = new HashMap<Integer, P>();
  }

  public synchronized void objectRemoved(P page) {
    if(page.isDirty()) {
      writeAccess++;
      page.setDirty(false);
      file.put(page.getID(), page);
    }
  }

  /**
   * Reads the page with the given id from this file.
   * 
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  @Override
  public synchronized P readPage(int pageID) {
    // try to get from cache
    P page = super.readPage(pageID);

    // get from file and put to cache
    if(page == null) {
      readAccess++;
      page = file.get(pageID);
      if(page != null) {
        cache.put(page);

      }
    }
    return page;
  }

  /**
   * Deletes the node with the specified id from this file.
   * 
   * @param pageID the id of the node to be deleted
   */
  @Override
  public synchronized void deletePage(int pageID) {
    // put id to empty nodes and
    // delete from cache
    super.deletePage(pageID);

    // delete from file
    writeAccess++;
    file.remove(pageID);
  }

  /**
   * Clears this PageFile.
   */
  @Override
  public void clear() {
    super.clear();
    file.clear();
  }
}
