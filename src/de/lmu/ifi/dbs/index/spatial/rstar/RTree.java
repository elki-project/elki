package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.Entry;
import de.lmu.ifi.dbs.index.spatial.LeafEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RTree is a spatial index structure based on the concepts of the R*-Tree. Apart from organizing the objects
 * it also provides several methods to search for certain object in the
 * structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTree<T extends NumberVector> extends AbstractRTree<T> {
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
  public RTreeNode getRoot() {
    return file.readPage(ROOT_NODE_ID);
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  protected boolean hasOverflow(RTreeNode node) {
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
  protected int computeHeight() {
    RTreeNode node = getRoot();
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
  protected void createEmptyRoot(int dimensionality) {
    RTreeNode root = createNewLeafNode(leafCapacity);
    file.writePage(root);
    this.height = 1;
  }

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   *
   * @param objects the data objects to be indexed
   */
  protected final void bulkLoad(List<T> objects) {
    StringBuffer msg = new StringBuffer();

    // root is leaf node
    if ((double) objects.size() / (leafCapacity - 1.0) <= 1) {
      RTreeNode root = createNewLeafNode(leafCapacity);
      file.writePage(root);
      createRoot(root, objects);
      height = 1;
      if (DEBUG) {
        msg.append("\n  numNodes = 1");
      }
    }

    // root is directory node
    else {
      RTreeNode root = createNewDirectoryNode(dirCapacity);
      file.writePage(root);

      // create leaf nodes
      RTreeNode[] nodes = createLeafNodes(objects);

      int numNodes = nodes.length;
      if (DEBUG) {
        msg.append("\n  numLeafNodes = ").append(numNodes);
      }
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
      if (DEBUG) {
        msg.append("\n  numNodes = ").append(numNodes);
      }
    }
    if (DEBUG) {
      msg.append("\n  height = ").append(height);
      logger.fine(msg.toString() + "\n");
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RTreeNode createNewLeafNode(int capacity) {
    return new RTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RTreeNode createNewDirectoryNode(int capacity) {
    return new RTreeNode(file, capacity, false);
  }

  /**
   * Creates and returns the directory nodes for bulk load.
   *
   * @param nodes the nodes to be inserted
   * @return the directory nodes containing the nodes
   */
  private RTreeNode[] createDirectoryNodes(RTreeNode[] nodes) {
    int minEntries = dirMinimum;
    int maxEntries = dirCapacity - 1;

    ArrayList<RTreeNode> result = new ArrayList<RTreeNode>();
    while (nodes.length > 0) {
      StringBuffer msg = new StringBuffer();

      // get the split axis and split point
      BulkSplit split = new BulkSplit(nodes, minEntries, maxEntries);
      int splitAxis = split.splitAxis;
      int splitPoint = split.splitPoint;
      if (DEBUG) {
        msg.append("\nsplitAxis ").append(splitAxis);
        msg.append("\nsplitPoint ").append(splitPoint);
      }

      // sort in the right dimension
      final SpatialComparator comp = new SpatialComparator();
      comp.setCompareDimension(splitAxis);
      comp.setComparisonValue(SpatialComparator.MIN);
      Arrays.sort(nodes, comp);

      // create node
      RTreeNode dirNode = createNewDirectoryNode(dirCapacity);
      file.writePage(dirNode);
      result.add(dirNode);

      // insert data
      for (int i = 0; i < splitPoint; i++) {
        dirNode.addNode(nodes[i]);
      }

      // copy array
      RTreeNode[] rest = new RTreeNode[nodes.length - splitPoint];
      System.arraycopy(nodes, splitPoint, rest, 0, nodes.length - splitPoint);
      nodes = rest;
      if (DEBUG) {
        msg.append("\nrestl. nodes # ").append(nodes.length);
      }

      // write to file
      file.writePage(dirNode);
      if (DEBUG) {
        msg.append("\npageNo ").append(dirNode.getID());
        logger.finer(msg.toString() + "\n");
      }
    }

    logger.info("numDirPages " + result.size());
    return result.toArray(new RTreeNode[result.size()]);
  }

  /**
   * Returns a leaf root node for bulk load
   *
   * @param root    the new root node
   * @param objects the data objects to be inserted
   * @return the root node
   */
  private RTreeNode createRoot(RTreeNode root, List<T> objects) {
    // insert data
    for (T object : objects) {
      LeafEntry entry = new LeafEntry(object.getID(), getValues(object));
      root.addLeafEntry(entry);
    }

    // write to file
    file.writePage(root);
    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\npageNo ").append(root.getID());
      logger.finer(msg.toString() + "\n");
    }

    return root;
  }

  /**
   * Returns a directory root node for bulk load
   *
   * @param root  the new root node
   * @param nodes the objects (nodes or data objects) to be inserted
   * @return the root node
   */
  private RTreeNode createRoot(RTreeNode root, RTreeNode[] nodes) {
    // insert data
    for (RTreeNode node : nodes) {
      root.addNode(node);
    }

    // write to file
    file.writePage(root);

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\npageNo ").append(root.getID());
      logger.finer(msg.toString() + "\n");
    }

    return root;
  }
}
