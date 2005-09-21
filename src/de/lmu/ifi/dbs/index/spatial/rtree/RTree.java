package de.lmu.ifi.dbs.index.spatial.rtree;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.SpatialComparator;
import de.lmu.ifi.dbs.index.spatial.SpatialNode;
import de.lmu.ifi.dbs.index.spatial.SpatialObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RTree is a spatial index structure. Apart from organizing the objects
 * it also provides several methods to search for certain object in the
 * structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTree<T extends RealVector> extends AbstractRTree<T> {
  /**
   * Creates a new RTree from an existing persistent file.
   *
   * @param fileName  the name of the file storing the RTree
   * @param cacheSize the size of the cache in bytes
   */
  public RTree(String fileName, int cacheSize) {
    super(fileName, cacheSize);
  }

  /**
   * Creates a new RTree with the specified parameters.
   *
   * @param dimensionality the dimensionality of the data objects to be indexed
   * @param fileName       the name of the file for storing the entries,
   *                       if this parameter is null all entries will be hold in
   *                       main memory
   * @param pageSize       the size of a page in Bytes
   * @param cacheSize      the size of the cache in Bytes
   */
  public RTree(int dimensionality, String fileName, int pageSize,
               int cacheSize) {

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
  public RTree(final List<T> objects, final String fileName,
               final int pageSize, final int cacheSize) {

    super(objects, fileName, pageSize, cacheSize);
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  public SpatialNode getRoot() {
    return file.readPage(ROOT_NODE_ID);
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
    else
      return node.getNumEntries() == dirCapacity;
  }

  /**
   * Computes the height of this RTree. Is called by the constructur.
   * and should be overwritten by subclasses if necessary.
   *
   * @return the height of this RTree
   */
  int computeHeight() {
    AbstractNode node = (AbstractNode) getRoot();
    int height = 1;

    // compute height
    while (!node.isLeaf() && node.getNumEntries() != 0) {
      Entry entry = node.entries[0];
      node = getNode(entry.getID());
      height++;
    }
    return height;
  }

  /**
   * Creates an empty root node and writes it to file. Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  void createEmptyRoot(int dimensionality) {
    AbstractNode root = createNewLeafNode(leafCapacity);
    file.writePage(root);
    this.height = 1;
  }

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   */
  void bulkLoad(Data[] data) {
    StringBuffer msg = new StringBuffer();

    // root is leaf node
    if ((double) data.length / (leafCapacity - 1.0) <= 1) {
      AbstractNode root = createNewLeafNode(leafCapacity);
      file.writePage(root);
      createRoot(root, data);
      height = 1;
      msg.append("\n  numNodes = 1");
    }

    // root is directory node
    else {
      AbstractNode root = createNewDirectoryNode(dirCapacity);
      file.writePage(root);

      // create leaf nodes
      AbstractNode[] nodes = createLeafNodes(data);
      int numNodes = nodes.length;
      msg.append("\n  numLeafNodes = ").append(numNodes);
      height = 1;

      // create directory nodes
      while (nodes.length > (dirCapacity - 1)) {
        nodes = createDirectoryNodes(nodes);
        numNodes += nodes.length;
        height++;
      }

      // create root
      createRoot(root, nodes);
      numNodes++;
      height++;
      msg.append("\n  numNodes = ").append(numNodes);
    }
    msg.append("\n  height = ").append(height);
    logger.info(msg.toString() + "\n");
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

  /**
   * Creates and returns the directory nodes for bulk load.
   *
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private AbstractNode[] createDirectoryNodes(AbstractNode[] nodes) {
    int minEntries = dirMinimum;
    int maxEntries = dirCapacity - 1;

    ArrayList<AbstractNode> result = new ArrayList<AbstractNode>();
    while (nodes.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      int splitAxis = SplitDescription.chooseBulkSplitAxis(nodes);
      int splitPoint = SplitDescription.chooseBulkSplitPoint(nodes.length, minEntries, maxEntries);
      msg.append("\nsplitAxis ").append(splitAxis);
      msg.append("\nsplitPoint ").append(splitPoint);

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      comp.setComparisonValue(SpatialComparator.MIN);
      Arrays.sort(nodes, comp);

      // create node
      AbstractNode dirNode = createNewDirectoryNode(dirCapacity);
      file.writePage(dirNode);
      result.add(dirNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        dirNode.addEntry(nodes[i]);
      }

      // copy array
      AbstractNode[] rest = new AbstractNode[nodes.length - splitPoint];
      System.arraycopy(nodes, splitPoint, rest, 0, nodes.length - splitPoint);
      nodes = rest;
      msg.append("\nrestl. nodes # ").append(nodes.length);

      // write to file
      file.writePage(dirNode);
      msg.append("\npageNo ").append(dirNode.getID());
      logger.fine(msg.toString() + "\n");
    }

    logger.info("numDirPages " + result.size());
    return result.toArray(new AbstractNode[result.size()]);
  }

  /**
   * Returns the root node for bulk load
   *
   * @param root    the new root node
   * @param objects the objects (nodes or data objects) to be inserted
   * @return the root node
   */
  private AbstractNode createRoot(AbstractNode root, SpatialObject[] objects) {
    StringBuffer msg = new StringBuffer();

    // insert data
    for (SpatialObject object : objects) {
      root.addEntry(object);
    }

    // write to file
    file.writePage(root);
    msg.append("\npageNo ").append(root.getID());
    logger.fine(msg.toString() + "\n");

    return root;
  }
}
