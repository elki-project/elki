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
   * Creates a new MemoryPageFile with the specified cache.
   *
   * @param cacheSize the size of the cache of this PageFile.
   * @param cacheType the type of the cache
   */
  protected MemoryPageFile(int cacheSize, String cacheType) {
    super(cacheSize, cacheType);
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
   * @see PageFile#readNode(int)
   */
  public synchronized Node readNode(int pageID) {
    // try to get from cache
    Node node = super.readNode(pageID);
    if (node != null) {
      return node;
    }

    // get from file
    ioAccess++;
    node = file.get(new Integer(pageID));
    return node;
  }

  /**
   * @see PageFile#deleteNode(int)
   */
  protected synchronized void deleteNode(int pageID) {
    // put id to empty pages and delete from cache
    super.deleteNode(pageID);

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
