package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.*;

import java.util.List;

/**
 * FlatRTree is a spatial index structure based on a RTree
 * but with a flat directory. Apart from organizing the objects it also provides several
 * methods to search for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FlatRTree<T extends RealVector> extends AbstractRTree<T> {
  private AbstractNode root;

  /**
   * Creates a new FlatRTree from an existing file.
   *
   * @param fileName  the name of the file for storing the entries,
   * @param cacheSize the size of the cache in bytes
   */
  public FlatRTree(String fileName, int cacheSize) {
    super(fileName, cacheSize);

    // reconstruct root
    int nextPageID = file.getNextPageID();
    root = createNewDirectoryNode(nextPageID);
    for (int i = 1; i < nextPageID; i++) {
      AbstractNode node = file.readPage(i);
      root.addEntry(node);
    }

    logger.info(getClass() + "\n" + " root: " + root + " with " + nextPageID + " leafNodes.");
  }

  /**
   * Creates a new FlatRTree with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be indexed
   * @param fileName       the name of the file for storing the entries,
   *                       if this parameter is null all entries will be hold in
   *                       main memory
   * @param pageSize       the size of a page in Bytes
   * @param cacheSize      the size of the cache in Bytes
   */
  public FlatRTree(int dimensionality, String fileName, int pageSize, int cacheSize) {
    super(dimensionality, fileName, pageSize, cacheSize);
  }

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param objects   the vector objects to be indexed
   * @param fileName  the name of the file for storing the entries,
   *                  if this parameter is null all entries will be hold in
   *                  main memory
   * @param pageSize  the size of a page in bytes
   * @param cacheSize the size of the cache (must be >= 1)
   */
  public FlatRTree(final List<T> objects, final String fileName, final int pageSize, final int cacheSize) {
    super(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public synchronized void insert(DoubleVector o) {
    Data data = new Data(o.getID(), o.getValues(), null);
    super.insert(data, 1);
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  public SpatialNode getRoot() {
    return root;
  }

  /**
   * Returns the height of this FlatRTree. Is called by the constructur.
   *
   * @return 2
   */
  int computeHeight() {
    return 2;
  }

  /**
   * Performs a bulk load on this FlatRTree with the specified data.
   */
  void bulkLoad(Data[] data) {
    StringBuffer msg = new StringBuffer();

    // create leaf nodes
    file.setNextPageID(ROOT_NODE_ID + 1);
    AbstractNode[] nodes = createLeafNodes(data);
    int numNodes = nodes.length;
    logger.info("\n  numLeafNodes = " + numNodes);

    // create root
    root = createNewDirectoryNode(nodes.length);
    root.nodeID = ROOT_NODE_ID;
    for (AbstractNode node : nodes) {
      root.addEntry(node);
    }
    numNodes++;
    this.height = 2;

    msg.append("\n  root = ").append(getRoot());
    msg.append("\n  numNodes = ").append(numNodes);
    msg.append("\n  height = ").
    append(height);
    logger.info(msg.toString() + "\n");
  }

  /**
   * Creates an empty root node.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  void createEmptyRoot(int dimensionality) {
    root = createNewDirectoryNode(dirCapacity);
    root.nodeID = ROOT_NODE_ID;

    file.setNextPageID(ROOT_NODE_ID + 1);
    AbstractNode leaf = createNewLeafNode(leafCapacity);
    file.writePage(leaf);

    MBR mbr = new MBR(new double[dimensionality], new double[dimensionality]);
    root.entries[root.numEntries++] = new DirectoryEntry(leaf.getID(), mbr);
    leaf.parentID = ROOT_NODE_ID;
    leaf.index = root.numEntries - 1;
    file.writePage(leaf);

    this.height = 2;
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  boolean hasOverflow(AbstractNode node) {
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;
    else {
      Entry[] tmp = node.entries;
      node.entries = new Entry[tmp.length + 1];
      System.arraycopy(tmp, 0, node.entries, 0, tmp.length);
      return false;
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  AbstractNode createNewLeafNode(int capacity) {
    return new Node(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  AbstractNode createNewDirectoryNode(int capacity) {
    return new Node(file, capacity, false);
  }

}
