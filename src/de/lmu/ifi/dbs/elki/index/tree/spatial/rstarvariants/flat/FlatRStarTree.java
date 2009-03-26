package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * FlatRTree is a spatial index structure based on a R*-Tree
 * but with a flat directory. Apart from organizing the objects it also provides several
 * methods to search for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert 
 */
public final class FlatRStarTree<O extends NumberVector<O,? >> extends AbstractRStarTree<O, FlatRStarTreeNode, SpatialEntry> {

  /**
   * The root of this flat RTree.
   */
  private FlatRStarTreeNode root;

  /**
   * Creates a new FlatRTree.
   */
  public FlatRStarTree() {
    super();
    //this.debug = true;
  }

  /**
   * Initializes the flat RTree from an existing persistent file.
   */
  @Override
  protected void initializeFromFile() {
    super.initializeFromFile();

    // reconstruct root
    int nextPageID = file.getNextPageID();
    root = createNewDirectoryNode(nextPageID);
    for (int i = 1; i < nextPageID; i++) {
      FlatRStarTreeNode node = file.readPage(i);
      root.addDirectoryEntry(createNewDirectoryEntry(node));
    }

    if (logger.isLoggable(LogLevel.FINE)) {
      debugFine(getClass() + "\n" + " root: " + root + " with " + nextPageID + " leafNodes.");
    }
  }

  /**
   * Returns the root node of this RTree.
   *
   * @return the root node of this RTree
   */
  @Override
  public FlatRStarTreeNode getRoot() {
    return root;
  }

  /**
   * Returns the height of this FlatRTree.
   *
   * @return 2
   */
  @Override
  protected int computeHeight() {
    return 2;
  }

  /**
   * Performs a bulk load on this RTree with the specified data.
   * Is called by the constructor
   * and should be overwritten by subclasses if necessary.
   *
   * @param objects the data objects to be indexed
   */
  @Override
  protected void bulkLoad(List<O> objects) {
    // create leaf nodes
    //noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getID() + 1);
    List<FlatRStarTreeNode> nodes = createLeafNodes(objects);
    int numNodes = nodes.size();
    if (logger.isLoggable(LogLevel.FINE)) {
      debugFine("\n  numLeafNodes = " + numNodes);
    }

    // create root
    root = createNewDirectoryNode(numNodes);
    root.setID(getRootEntry().getID());
    for (FlatRStarTreeNode node : nodes) {
      root.addDirectoryEntry(createNewDirectoryEntry(node));
    }
    numNodes++;
    setHeight(2);

    if (logger.isLoggable(LogLevel.FINE)) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n  root = ").append(getRoot());
      msg.append("\n  numNodes = ").append(numNodes);
      msg.append("\n  height = ").append(getHeight());
      debugFine(msg.toString() + "\n");
    }
    if (extraIntegrityChecks) {
      getRoot().integrityCheck();
    }
  }

  @Override
  protected void createEmptyRoot(O object) {
    root = createNewDirectoryNode(dirCapacity);
    root.setID(getRootEntry().getID());

    //noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getID() + 1);
    FlatRStarTreeNode leaf = createNewLeafNode(leafCapacity);
    file.writePage(leaf);
    HyperBoundingBox mbr = new HyperBoundingBox(new double[object.getDimensionality()], new double[object.getDimensionality()]);
    //noinspection unchecked
    root.addDirectoryEntry(new SpatialDirectoryEntry(leaf.getID(), mbr));

    setHeight(2);
  }

  /**
   * Returns true if in the specified node an overflow occurred, false otherwise.
   *
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  @Override
  protected boolean hasOverflow(FlatRStarTreeNode node) {
    if (node.isLeaf())
      return node.getNumEntries() == leafCapacity;
    else if (node.getNumEntries() == node.getCapacity()) {
      node.increaseEntries();
    }
    return false;
  }

  /**
   * Returns true if in the specified node an underflow occurred, false otherwise.
   *
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false otherwise
   */
  @Override
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
  @Override
  protected FlatRStarTreeNode createNewLeafNode(int capacity) {
    return new FlatRStarTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected FlatRStarTreeNode createNewDirectoryNode(int capacity) {
    return new FlatRStarTreeNode(file, capacity, false);
  }

  @Override
  protected SpatialEntry createNewLeafEntry(O o) {
    return new SpatialLeafEntry(o.getID(), getValues(o));
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(FlatRStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getID(), node.mbr());
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(SpatialEntry entry) {
    // do nothing
  }

  /**
   * Performs necessary operations after deleting the specified object.
   *
   * @param o the object to be deleted
   */
  @Override
  protected void postDelete(O o) {
    // do nothing
  }
}
