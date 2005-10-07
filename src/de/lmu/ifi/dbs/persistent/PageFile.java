package de.lmu.ifi.dbs.persistent;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class implementing general methods of a PageFile.
 * A PageFile stores objects that implement the <code>Page</code> inetrface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class PageFile<T extends Page> implements CachedFile<T> {
  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level level = Level.OFF;

  /**
   * The cache of this file.
   */
  protected Cache<T> cache;

  /**
   * A stack holding the empty page ids.
   */
  protected Stack<Integer> emptyPages;

  /**
   * The last page ID.
   */
  protected int nextPageID;

  /**
   * The I/O-Access of this file.
   */
  protected long ioAccess;

  /**
   * The size of a page in Bytes.
   */
  protected int pageSize;

  /**
   * Creates a new PageFile.
   */
  protected PageFile() {
    initLogger();

    this.emptyPages = new Stack<Integer>();
    this.nextPageID = 0;
    this.ioAccess = 0;
  }

  /**
   * Returns the I/O-Access of this file.
   */
  public final long getIOAccess() {
    return ioAccess;
  }

  /**
   * Resets the I/O-Access of this file and clears the cache.
   */
  public final void resetIOAccess() {
    cache.flush();
    this.ioAccess = 0;
  }

  /**
   * Sets the id of the given page.
   *
   * @param page the page to set the id
   */
  public void setPageID(T page) {
    if (page.getID() == null) {
      Integer pageID = getNextEmptyPageID();

      if (pageID == null) {
        page.setID(nextPageID++);
      }
      else
        page.setID(pageID);
    }
  }

  /**
   * Writes a page into this file.
   * The method tests if the page has already an id, otherwise
   * a new id is assigned and returned.
   *
   * @param page the page to be written
   * @return the id of the page
   */
  public synchronized final int writePage(T page) {
    // set page ID
    setPageID(page);
    // put node into cache
    cache.put(page);
    return page.getID();
  }

  /**
   * Reads the page with the given id from this file.
   *
   * @param pageID the id of the page to be returned
   * @return the page with the given pageId
   */
  public T readPage(int pageID) {
    // try to get from cache
    return cache.get(pageID);
  }

  /**
   * Deletes the node with the specified id from this file.
   *
   * @param pageID the id of the node to be deleted
   */
  public void deletePage(int pageID) {
    // put id to empty nodes
    emptyPages.push(pageID);

    // delete from cache
    cache.remove(pageID);
  }

  /**
   * Closes this file.
   */
  public void close() {
    cache.flush();
  }

  /**
   * Clears this PageFile.
   */
  public void clear() {
    cache.clear();
  }

  /**
   * Initializes the cache.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Byte
   * @param cache     the class of the cache to be used
   */
  void initCache(int pageSize, int cacheSize, Cache<T> cache) {
    if (pageSize <= 0)
      throw new IllegalStateException("pagesize <= 0!");

    int pagesInCache = cacheSize / pageSize;
    logger.info("Number of pages in cache " + pagesInCache);

//    if (pagesInCache <= 0)
//      throw new IllegalArgumentException("Cache size of " + cacheSize + " Bytes is chosen too small: " +
//                                         cacheSize + "/" + pageSize + " = " + pagesInCache);

    this.pageSize = pageSize;
    this.cache = cache;
    this.cache.initialize(pagesInCache, this);
  }

  /**
   * Returns the next empty page id.
   *
   * @return the next empty page id
   */
  private Integer getNextEmptyPageID() {
    if (!emptyPages.empty())
      return emptyPages.pop();
    else
      return null;
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }

  /**
   * Returns the next page id.
   * @return the next page id
   */
  public int getNextPageID() {
    return nextPageID;
  }

  /**
   * Sets the next page id.
   * @param nextPageID the next page id to be set
   */
  public void setNextPageID(int nextPageID) {
    this.nextPageID = nextPageID;
  }

}
