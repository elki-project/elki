package de.lmu.ifi.dbs.elki.persistent;

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a PageFile that simulates I/O-access.<br>
 * Implemented as a Map with keys representing the ids of the saved pages.
 * 
 * @author Elke Achtert
 * 
 * @param <P> Page type
 */
public class MemoryPageFile<P extends Page> extends AbstractStoringPageFile<P> {
  /**
   * Holds the pages.
   */
  private final Map<Integer, P> file;

  /**
   * Creates a new MemoryPageFile that is supported by a cache with the
   * specified parameters.
   * 
   * @param pageSize the size of a page in Bytes
   */
  public MemoryPageFile(int pageSize) {
    super(pageSize);
    this.file = new HashMap<Integer, P>();
  }

  @Override
  public synchronized P readPage(int pageID) {
    readAccess++;
    return file.get(pageID);
  }
  
  @Override
  protected void writePage(Integer pageID, P page) {
    writeAccess++;
    file.put(pageID, page);
    page.setDirty(false);
  }

  @Override
  public synchronized void deletePage(int pageID) {
    // put id to empty nodes and
    // delete from cache
    super.deletePage(pageID);

    // delete from file
    writeAccess++;
    file.remove(pageID);
  }

  @Override
  public void clear() {
    file.clear();
  }
}