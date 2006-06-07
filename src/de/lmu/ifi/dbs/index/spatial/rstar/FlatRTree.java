package de.lmu.ifi.dbs.index.spatial.rstar;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.*;

import java.util.List;

/**
 * FlatRTree is a spatial index structure based on a R*-Tree
 * but with a flat directory. Apart from organizing the objects it also provides several
 * methods to search for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FlatRTree<O extends NumberVector> extends AbstractRTree<O> {
  /**
   * The root of this flat RTree.
   */
  private RTreeNode root;

  /**
   * Creates a new FlatRTree.
   */
  public FlatRTree() {
    super();
  }

  /**
   * Initializes the R-FlatRTree from an existing persistent file.
   */
  public void initFromFile() {
    super.initFromFile();

    // reconstruct root
    int nextPageID = file.getNextPageID();
    root = createNewDirectoryNode(nextPageID);
    for (int i = 1; i < nextPageID; i++) {
      RTreeNode node = file.readPage(i);
      root.addNode(node);
    }

    if (DEBUG) {
      logger.fine(getClass() + "\n" + " root: " + root + " with " + nextPageID + " leafNodes.");
    }
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  public RTreeNode getRoot() {
    return root;
  }

  /**
   * Returns the height of this FlatRTree. Is called by the constructur.
   *
   * @return 2
   */
  protected int computeHeight() {
    return 2;
  }

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructur
   * and should be overwritten by subclasses if necessary.
   *
   * @param objects  the data objects to be indexed
   */
  protected final void bulkLoad(List<O> objects) {
    // create leaf nodes
    //noinspection PointlessArithmeticExpression
    file.setNextPageID(ROOT_NODE_ID + 1);
    List<RTreeNode> nodes = createLeafNodes(objects);
    int numNodes = nodes.size();
    if (DEBUG) {
      logger.fine("\n  numLeafNodes = " + numNodes);
    }

    // create root
    root = createNewDirectoryNode(numNodes);
    root.nodeID = ROOT_NODE_ID;
    for (RTreeNode node : nodes) {
      root.addNode(node);
    }
    numNodes++;
    this.height = 2;

    if (DEBUG) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n  root = ").append(getRoot());
      msg.append("\n  numNodes = ").append(numNodes);
      msg.append("\n  height = ").append(height);
      logger.fine(msg.toString() + "\n");
    }
  }

  /**
   * Creates an empty root node.
   *
   * @param dimensionality the dimensionality of the data objects to be stored
   */
  protected void createEmptyRoot(int dimensionality) {
    root = createNewDirectoryNode(dirCapacity);
    root.nodeID = ROOT_NODE_ID;

    //noinspection PointlessArithmeticExpression
    file.setNextPageID(ROOT_NODE_ID + 1);
    RTreeNode leaf = createNewLeafNode(leafCapacity);
    file.writePage(leaf);

    MBR mbr = new MBR(new double[dimensionality], new double[dimensionality]);
    root.entries[root.numEntries++] = createNewDirectoryEntry(leaf.getID(), mbr);
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
  protected boolean hasOverflow(RTreeNode node) {
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;
    else {
      SpatialEntry[] tmp = node.entries;
      node.entries = new SpatialEntry[tmp.length + 1];
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
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  protected LeafEntry createNewLeafEntry(int id, double[] values) {
    return new LeafEntry(id, values);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  protected DirectoryEntry createNewDirectoryEntry(int id, MBR mbr) {
    return new DirectoryEntry(id, mbr);
  }
}
