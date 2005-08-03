package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.Cache;
import de.lmu.ifi.dbs.caching.CachedFile;
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
abstract class RTreeFile implements CachedFile {

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
   * The size of a node in byte.
   */
  protected int nodeSize;

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
  protected boolean flatDirectory;

  /**
   * The cache of this PageFile.
   */
  protected Cache cache;

  /**
   * A stack holding the empty node ids.
   */
  protected Stack<Integer> emptyNodes;

  /**
   * The last node ID.
   */
  private int lastNodeID;

  /**
   * The I/O-Access of this PageFile.
   */
  protected int ioAccess;

  /**
   * Creates a new PageFile object.
   */
  protected RTreeFile() {
    initLogger();
  }

  /**
   * Creates a new PageFile object with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be stored in this file
   * @param nodeSize       the size of a node in byte
   * @param cacheSize      the size of the cache in byte
   * @param cacheType      the type of the cache
   * @param flatDirectory  a boolean that indicates a flat directory
   */
  protected RTreeFile(int dimensionality, int nodeSize,
                     int cacheSize, String cacheType,
                     boolean flatDirectory) {

    initLogger();

    this.emptyNodes = new Stack<Integer>();
    this.lastNodeID = 0;
    this.ioAccess = 0;
    this.dimensionality = dimensionality;
    this.flatDirectory = flatDirectory;

    // capacity: entries per page
    // overhead = typ(4), index(4), numEntries(4), parentID(4), id(4)
    double overhead = 20;
    if (nodeSize - overhead < 0)
      throw new RuntimeException("Node size of " + nodeSize + " Bytes is chosen too small!");

    // capacity = (pageSize - overhead) / (childID + childMBR) + 1
    this.capacity = (int) (nodeSize - overhead) / (4 + 16 * dimensionality);
    logger.info("capacity " + capacity);

    if (capacity <= 1)
      throw new RuntimeException("Node size of " + nodeSize + " Bytes is chosen too small!");

    // minimum entries per page
    int minimum = (int) Math.round((capacity - 1) * 0.5);
    if (minimum < 2)
      this.minimum = 2;
    else
      this.minimum = minimum;

    // exact pagesize
    this.nodeSize = (int) (overhead + capacity * (4 + 16 * dimensionality));
    logger.info("nodeSize " + this.nodeSize);

    // cache
    initCache(cacheSize, cacheType);
  }

  /**
   * Initializes the cache.
   *
   * @param cacheSize the size of the cache in byte
   * @param cacheType the type of the cache
   */
  protected void initCache(int cacheSize, String cacheType) {
    int cacheNo = cacheSize / this.nodeSize;
    logger.info("cachesize " + cacheNo);

    if (cacheNo <= 0)
      throw new IllegalArgumentException("Cache size of " + cacheSize + " Bytes is chosen too small: " +
                                         cacheSize + "/" + nodeSize + " = " + cacheNo);

    if (cacheType.equals(LRU_CACHE)) {
      this.cache = new LRUCache(cacheNo, this);
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
   * Resets the I/O-Access of this file and clears the cache.
   */
  protected final void resetIOAccess() {
    cache.clear();
    this.ioAccess = 0;
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
   * Sets the page id of the given node.
   *
   * @param node the node to set the page id
   */
  protected void setPageID(Node node) {
    if (node.nodeID < 0) {
      int pageID = getNextEmptyPageID();

      if (pageID < 0)
        node.nodeID = lastNodeID++;
      else
        node.nodeID = pageID;
    }
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
    return node.nodeID;
  }

  /**
   * Reads the node with the given pageId from this PageFile.
   *
   * @param pageID the id of the node to be returned
   * @return the node with the given pageId
   */
  protected abstract Node readNode(int pageID);

  /**
   * Deletes the node with the specified id from this PageFile.
   *
   * @param pageID the id of the node to be deleted
   */
  protected abstract void deleteNode(int pageID);

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
    if (!emptyNodes.empty())
      return ((Integer) emptyNodes.pop()).intValue();
    else
      return -1;
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(RTreeFile.class.toString());
    logger.setLevel(level);
  }


}
