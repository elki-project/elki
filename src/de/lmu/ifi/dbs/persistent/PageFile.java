package de.lmu.ifi.dbs.persistent;

import java.util.Stack;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

/**
 * Abstract class implementing general methods of a PageFile.
 * A PageFile stores objects that implement the <code>Page</code> inetrface.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class PageFile<P extends Page> extends AbstractLoggable implements CachedFile<P> {
//  /**
//   * Holds the class specific debug status.
//   */
//  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
////  private static final boolean DEBUG = true;
//
//  /**
//   * The logger of this class.
//   */
//  @SuppressWarnings({"FieldCanBeLocal"})
//  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The cache of this file.
   */
  protected Cache<P> cache;

  /**
   * A stack holding the empty page ids.
   */
  protected Stack<Integer> emptyPages;

  /**
   * The last page ID.
   */
  protected int nextPageID;

  /**
   * The read I/O-Access of this file.
   */
  protected long readAccess;

  /**
   * The write I/O-Access of this file.
   */
  protected long writeAccess;

  /**
   * The size of a page in Bytes.
   */
  protected int pageSize;

  /**
   * Creates a new PageFile.
   */
  protected PageFile() {
	  super(LoggingConfiguration.DEBUG);
    this.emptyPages = new Stack<Integer>();
    this.nextPageID = 0;
    this.readAccess = 0;
    this.writeAccess = 0;
  }

  /**
   * Returns the physical read I/O-Access of this file.
   */
  public final long getPhysicalReadAccess() {
    return readAccess;
  }

  /**
   * Returns the physical write I/O-Access of this file.
   */
  public final long getPhysicalWriteAccess() {
    return writeAccess;
  }

  /**
   * Returns the logical read I/O-Access of this file.
   */
  public final long getLogicalPageAccess() {
    return cache.getPageAccess();
  }

  /**
   * Resets the counters for page accesses of this file and flushes the cache.
   */
  public final void resetPageAccess() {
    cache.flush();
    this.readAccess = 0;
    this.writeAccess = 0;
    cache.resetPageAccess();
  }

  /**
   * Sets the id of the given page.
   *
   * @param page the page to set the id
   */
  public void setPageID(P page) {
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
  public synchronized final int writePage(P page) {
    // set page ID
    setPageID(page);
    // mark page as dirty
    page.setDirty(true);
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
  public P readPage(int pageID) {
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
   * Sets the maximum size of the cache of this file.
   *
   * @param cacheSize
   */
  public void setCacheSize(int cacheSize) {
    cache.setCacheSize(cacheSize / pageSize);
  }

  /**
   * Initializes the cache.
   *
   * @param pageSize  the size of a page in Bytes
   * @param cacheSize the size of the cache in Byte
   * @param cache     the class of the cache to be used
   */
  void initCache(int pageSize, int cacheSize, Cache<P> cache) {
    if (pageSize <= 0)
      throw new IllegalStateException("pagesize <= 0!");

    int pagesInCache = cacheSize / pageSize;
    if (this.debug) {
    	debugFine("Number of pages in cache " + pagesInCache);
//      logger.fine("Number of pages in cache " + pagesInCache);
    }

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
   * Returns the next page id.
   *
   * @return the next page id
   */
  public int getNextPageID() {
    return nextPageID;
  }

  /**
   * Sets the next page id.
   *
   * @param nextPageID the next page id to be set
   */
  public void setNextPageID(int nextPageID) {
    this.nextPageID = nextPageID;
  }

}
