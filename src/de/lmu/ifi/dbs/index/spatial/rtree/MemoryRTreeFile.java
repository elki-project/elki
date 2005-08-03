package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.caching.CachedFile;
import de.lmu.ifi.dbs.caching.Identifiable;

import java.util.HashMap;
import java.util.Map;

/**
 * A memory based implementation of a RTreeFile.<br>
 * Implemented as a Map with keys representing
 * the ids of the saved nodes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MemoryRTreeFile extends RTreeFile {
  /**
   * Holds the nodes of the RTree.
   */
  private final Map<Integer, Node> file;

  /**
   * Creates a new MemoryRTreeFile with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be stored in this file
   * @param nodeSize       the size of a node in byte
   * @param cacheSize      the size of the cache in byte
   * @param cacheType      the type of the cache
   * @param flatDirectory  a boolean that indicates a flat directory
   */
  protected MemoryRTreeFile(int dimensionality, int nodeSize,
                           int cacheSize, String cacheType,
                           boolean flatDirectory) {

    super(dimensionality, nodeSize, cacheSize, cacheType, flatDirectory);

    this.file = new HashMap<Integer, Node>();
  }

  /**
   * @see CachedFile#write(de.lmu.ifi.dbs.caching.Identifiable)
   */
  public synchronized void write(Identifiable object) {
    ioAccess++;
    Node node = (Node) object;
    file.put(new Integer(node.nodeID), node);
  }


  /**
   * Reads the node with the given id from this file.
   *
   * @param nodeID the id of the node to be returned
   * @return the node with the given nodeID
   */
  public synchronized Node readNode(int nodeID) {
    // try to get from cache
    Node node = (Node) cache.get(nodeID);
    if (node != null) {
      return node;
    }

    // get from file and put to cache
    ioAccess++;
    node = file.get(new Integer(nodeID));
    cache.put(node);
    return node;
  }

   /**
   * Deletes the node with the specified id from this file.
   *
   * @param nodeID the id of the node to be deleted
   */
  protected synchronized void deleteNode(int nodeID) {
    // put id to empty nodes
    emptyNodes.push(new Integer(nodeID));

    // delete from cache
    cache.remove(nodeID);

    // delete from file
    ioAccess++;
     file.remove(new Integer(nodeID));
  }

  /**
   * @see RTreeFile#close()
   */
  protected void close() {
    // nothing to do
  }

  /**
   * @see RTreeFile#increaseRootNode()
   */
  protected int increaseRootNode() {
    return 1;
  }
}
