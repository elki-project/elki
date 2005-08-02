package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.CachedPageFile;
import de.lmu.ifi.dbs.caching.Page;

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a PageFile.<br>
 * Implemented as a Map with keys representing
 * the page numbers of the saved nodes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MemoryPageFile extends PageFile {
  /**
   * Holds the nodes of the RTree.
   */
  private final Map<Integer, Node> file;

  /**
   * Creates a new MemoryPageFile with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be stored in this file
   * @param pageSize       the size of a page in byte
   * @param cacheSize      the size of the cache in byte
   * @param cacheType      the type of the cache
   * @param flatDirectory  a boolean that indicates a flat directory
   */
  protected MemoryPageFile(int dimensionality, int pageSize,
                           int cacheSize, String cacheType,
                           boolean flatDirectory) {

    super(dimensionality, pageSize, cacheSize, cacheType, flatDirectory);

    this.file = new HashMap<Integer, Node>();
  }

  /**
   * @see CachedPageFile#write(de.lmu.ifi.dbs.caching.Page)
   */
  public synchronized void write(Page page) {
    ioAccess++;
    Node node = (Node) page;
    file.put(new Integer(node.pageID), node);
  }


  /**
   * Reads the node with the given pageId from this PageFile.
   *
   * @param pageID the id of the node to be returned
   * @return the node with the given pageId
   */
  public synchronized Node readNode(int pageID) {
    // try to get from cache
    Node node = (Node) cache.get(pageID);
    if (node != null) {
      return node;
    }

    // get from file and put to cache
    ioAccess++;
    node = file.get(new Integer(pageID));
    cache.put(node);
    return node;
  }

   /**
   * Deletes the node with the specified pageID from this PageFile.
   *
   * @param pageID the id of the node to be deleted
   */
  protected synchronized void deleteNode(int pageID) {
    // put id to empty pages
    emptyPages.push(new Integer(pageID));

    // delete from cache
    cache.remove(pageID);

    // delete from file
    ioAccess++;
     file.remove(new Integer(pageID));
  }

  /**
   * @see PageFile#close()
   */
  protected void close() {
    // nothing to do
  }

  /**
   * @see PageFile#increaseRootNode()
   */
  protected int increaseRootNode() {
    return 1;
  }
}
