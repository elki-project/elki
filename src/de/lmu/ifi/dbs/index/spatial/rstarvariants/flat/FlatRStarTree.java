package de.lmu.ifi.dbs.index.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.util.List;
import java.util.logging.Logger;

/**
 * FlatRTree is a spatial index structure based on a R*-Tree
 * but with a flat directory. Apart from organizing the objects it also provides several
 * methods to search for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public final class FlatRStarTree<O extends NumberVector> extends AbstractRStarTree<O, FlatRStarTreeNode, SpatialEntry> {
  /**
   * Holds the class specific debug status.
   */
  private static boolean DEBUG = LoggingConfiguration.DEBUG;
//  protected static boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The root of this flat RTree.
   */
  private FlatRStarTreeNode root;

  /**
   * Creates a new FlatRTree.
   */
  public FlatRStarTree() {
    super();
  }

  /**
   * Initializes the flat RTree from an existing persistent file.
   */
  protected void initializeFromFile() {
    super.initializeFromFile();

    // reconstruct root
    int nextPageID = file.getNextPageID();
    root = createNewDirectoryNode(nextPageID);
    for (int i = 1; i < nextPageID; i++) {
      FlatRStarTreeNode node = file.readPage(i);
      root.addDirectoryEntry(createNewDirectoryEntry(node));
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
  public FlatRStarTreeNode getRoot() {
    return root;
  }

  /**
   * Returns the height of this FlatRTree.
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
   * @param objects the data objects to be indexed
   */
  protected void bulkLoad(List<O> objects) {
    // create leaf nodes
    //noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getID() + 1);
    List<FlatRStarTreeNode> nodes = createLeafNodes(objects);
    int numNodes = nodes.size();
    if (DEBUG) {
      logger.fine("\n  numLeafNodes = " + numNodes);
    }

    // create root
    root = createNewDirectoryNode(numNodes);
    root.setID(getRootEntry().getID());
    for (FlatRStarTreeNode node : nodes) {
      root.addDirectoryEntry(createNewDirectoryEntry(node));
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
   * @see de.lmu.ifi.dbs.index.Index#createEmptyRoot(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  protected void createEmptyRoot(O object) {
    root = createNewDirectoryNode(dirCapacity);
    root.setID(getRootEntry().getID());

    //noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getID() + 1);
    FlatRStarTreeNode leaf = createNewLeafNode(leafCapacity);
    file.writePage(leaf);
    MBR mbr = new MBR(new double[object.getDimensionality()], new double[object.getDimensionality()]);
    //noinspection unchecked
    root.addDirectoryEntry(new SpatialDirectoryEntry(leaf.getID(), mbr));

    this.height = 2;
  }

  /**
   * Returns true if in the specified node an overflow occured, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occured, false otherwise
   */
  protected boolean hasOverflow(FlatRStarTreeNode node) {
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;
    else if (node.getNumEntries() == node.getCapacity()) {
      node.increaseEntries();
    }
    return false;
  }

  /**
   * Returns true if in the specified node an underflow occured, false otherwise.
   *
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occured, false otherwise
   */
  protected boolean hasUnderflow(FlatRStarTreeNode node) {
    if (node.isLeaf()) {
      return node.getNumEntries() < leafMinimum;
    }
    else return false;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected FlatRStarTreeNode createNewLeafNode(int capacity) {
    return new FlatRStarTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected FlatRStarTreeNode createNewDirectoryNode(int capacity) {
    return new FlatRStarTreeNode(file, capacity, false);
  }

  /**
   * @see AbstractRStarTree#createNewLeafEntry(NumberVector o)
   */
  protected SpatialEntry createNewLeafEntry(O o) {
    return new SpatialLeafEntry(o.getID(), getValues(o));
  }

  /**
   * @see AbstractRStarTree#createNewDirectoryEntry(AbstractRStarTreeNode node)
   */
  protected SpatialEntry createNewDirectoryEntry(FlatRStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getID(), node.mbr());
  }

  /**
   * @see AbstractRStarTree#createRootEntry()
   */
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }
}
