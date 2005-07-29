package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.Cache;
import de.lmu.ifi.dbs.caching.CachedPageFile;
import de.lmu.ifi.dbs.caching.LRUCache;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class implementing general methods of a PageFile.
 * A PageFile object stores the pages of a RTree. In this sense a page is a node of a RTree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
abstract class PageFile implements CachedPageFile {

  /**
   * A last recently used cache type.
   */
  public static final String LRU_CACHE = "LRU_CACHE";

  /**
   * Logger object for logging messages.
   */
  protected static Logger logger;

  /**
   * The loggerLevel for logging messages.
   */
  protected static Level level = Level.INFO;

  /**
   * The dimensionality of the stored data.
   */
  protected int dimensionality;

  /**
   * The capacity of a node (= 1 + maximum number of entries in a node).
   */
  protected int capacity;

  /**
   * The minimum number of entries in a node.
   */
  protected int minimum;

  /**
   * Indicates if the RTree has a flat directory or not.
   */
  private boolean flatDirectory;

  /**
   * The cache of this PageFile.
   */
  private final Cache cache;

  /**
   * A stack holding the empty page ids.
   */
  protected Stack<Integer> emptyPages;

  /**
   * The last page ID.
   */
  private int lastPageID;

  /**
   * The I/O-Access of this PageFile.
   */
  protected int ioAccess;

  /**
   * Creates a new PageFile object.
   *
   * @param cacheSize the size of the cache of this PageFile.
   * @param cacheType the type of the cache
   */
  protected PageFile(int cacheSize, String cacheType) {
    if (cacheSize <= 0)
      throw new IllegalArgumentException("The cache size must be > 0!");

    initLogger();
    this.emptyPages = new Stack<Integer>();
    this.lastPageID = 0;
    this.ioAccess = 0;

    if (cacheType.equals(LRU_CACHE)) {
      this.cache = new LRUCache(cacheSize, this);
    }
    else {
      throw new IllegalArgumentException("Unknown cache type: " + cacheType);
    }
  }

  /**
   * Returns the I/O-Access of this PageFile.
   */
  protected final int getIOAccess() {
    return ioAccess;
  }

  /**
   * Sets the I/O-Access of this PageFile to the specified value.
   *
   * @param ioAccess the value of the I/O-Access to be set
   */
  protected final void setIOAccess(int ioAccess) {
    this.ioAccess = ioAccess;
  }

  /**
   * Returns the dimensionality of the stored data.
   *
   * @return the dimensionality of the stored data
   */
  protected int getDimensionality() {
    return dimensionality;
  }

  /**
   * Returns the minimum number of entries in a node.
   *
   * @return the minimum number of entries in a node
   */
  protected int getMinimum() {
    return minimum;
  }

  /**
   * Returns the maximum number of entries in a node. </br>
   * If the directory of the RTree is flat <code>getMaximum</code> returns
   * only the maximum number of entries in a leaf node.
   *
   * @return the maximum number of entries in a node
   */
  protected int getMaximum() {
    return capacity - 1;
  }

  /**
   * Returns the capacity of a node.
   * Capacity is defined as maximum number of a node plus 1 for overflow. </br>
   * If the directory of the RTree is flat <code>getCapacity</code> returns
   * only the capacity of a leaf node.
   *
   * @return the capacity of a node
   */
  protected int getCapacity() {
    return capacity;
  }

  /**
   * Returns true if the directory of the RTree is flat, false otherwise.
   *
   * @return true if the directory of the RTree is flat, false otherwise
   */
  protected boolean isFlatDirectory() {
    return flatDirectory;
  }

  /**
   * Initializes the PageFile.
   *
   * @param dimensionality the dimension ofthe stored data
   * @param pageSize       the size of a page in bytes
   * @param flatDirectory  a boolean that indicates a flat directory
   */
  protected void initialize(int dimensionality, int pageSize, boolean flatDirectory) {
    this.dimensionality = dimensionality;
    this.flatDirectory = flatDirectory;

    // pageID (4), numEntries (4), parentID (4), index(4), reinsert (1/8)
    double overhead = 16.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Page size of " + pageSize + " Bytes is chosen too small!");

    // (pageSize - overhead) / (childNumber + childMBR)
    this.capacity = (int) (pageSize - overhead) / (4 + 16 * dimensionality) + 1;

    if (capacity <= 1)
      throw new RuntimeException("Page size of " + pageSize + " Bytes is chosen too small!");

    this.minimum = (int) Math.round((capacity - 1) * 0.5);
    if (this.minimum < 2)
      this.minimum = 2;
  }

  /**
   * Sets the page id of the given node.
   *
   * @param node the node to set the page id
   */
  protected void setPageID(Node node) {
    if (node.pageID < 0) {
      int pageID = getNextEmptyPageID();

      if (pageID < 0)
        node.pageID = lastPageID++;
      else
        node.pageID = pageID;
    }
  }

  /**
   * Reads the node with the given pageId from this PageFile.
   *
   * @param pageID the id of the node to be returned
   * @return the node with the given pageId
   */
  protected synchronized Node readNode(int pageID) {
    // try to get from cache
    Node node = (Node) cache.get(pageID);
    return node;
  }

  /**
   * Writes a node into this PageFile.
   * The method tests if the node has already a page id, otherwise
   * a new page number is assigned and returned.
   *
   * @param node the node to be written
   * @return the page id of the node
   */
  protected synchronized final int writeNode(Node node) {
    // set page ID
    setPageID(node);
    // put node into cache
    cache.put(node);
    return node.pageID;
  }

  /**
   * Deletes the node with the specified pageID from this PageFile.
   *
   * @param pageID the id of the node to be deleted
   */
  protected synchronized void deleteNode(int pageID){
    // put id to empty pages
    emptyPages.push(new Integer(pageID));

    // delete from cache
    cache.remove(pageID);
  }

  /**
   * Increases the capacity of the root node. This method should be called to
   * increase the capacity of a flat directory.
   *
   * @return the increasment of the capacity
   */
  protected abstract int increaseRootNode();

  /**
   * Closes this PageFile.
   */
  protected abstract void close();

  /**
   * Returns the next empty page id.
   *
   * @return the next empty page id
   */
  private int getNextEmptyPageID() {
    if (!emptyPages.empty())
      return ((Integer) emptyPages.pop()).intValue();
    else
      return -1;
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(PageFile.class.toString());
    logger.setLevel(level);
  }


}
