package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPair;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * FlatRTree is a spatial index structure based on a R*-Tree but with a flat
 * directory. Apart from organizing the objects it also provides several methods
 * to search for certain object in the structure and ensures persistence.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has FlatRStarTreeNode oneway - - contains
 * 
 * @param <O> object type
 */
public final class FlatRStarTree<O extends NumberVector<O, ?>> extends AbstractRStarTree<O, FlatRStarTreeNode, SpatialEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(FlatRStarTree.class);
  
  /**
   * The root of this flat RTree.
   */
  private FlatRStarTreeNode root;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   * @param insertionCandidates insertion candidate set size
   */
  public FlatRStarTree(Relation<O> relation, String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(relation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }

  /**
   * Initializes the flat RTree from an existing persistent file.
   */
  @Override
  public void initializeFromFile() {
    super.initializeFromFile();

    // reconstruct root
    int nextPageID = file.getNextPageID();
    root = createNewDirectoryNode(nextPageID);
    for(int i = 1; i < nextPageID; i++) {
      FlatRStarTreeNode node = file.readPage(i);
      root.addDirectoryEntry(createNewDirectoryEntry(node));
    }

    if(logger.isDebugging()) {
      logger.debugFine("root: " + root + " with " + nextPageID + " leafNodes.");
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
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(DBIDs ids) {
    List<SpatialPair<DBID, O>> spatialObjects = new ArrayList<SpatialPair<DBID, O>>(ids.size());
    for (DBID id : ids) {
      spatialObjects.add(new SpatialPair<DBID, O>(id, relation.get(id)));
    }
    // create leaf nodes
    // noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getEntryID() + 1);
    List<FlatRStarTreeNode> nodes = createLeafNodes(spatialObjects);
    int numNodes = nodes.size();
    if(logger.isDebugging()) {
      logger.debugFine("  numLeafNodes = " + numNodes);
    }

    // create root
    root = createNewDirectoryNode(numNodes);
    root.setPageID(getRootEntry().getEntryID());
    for(FlatRStarTreeNode node : nodes) {
      root.addDirectoryEntry(createNewDirectoryEntry(node));
    }
    numNodes++;
    setHeight(2);

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("  root = ").append(getRoot());
      msg.append("\n  numNodes = ").append(numNodes);
      msg.append("\n  height = ").append(getHeight());
      logger.debugFine(msg.toString() + "\n");
    }
    if(extraIntegrityChecks) {
      getRoot().integrityCheck();
    }
  }

  @Override
  protected void createEmptyRoot(SpatialEntry exampleLeaf) {
    root = createNewDirectoryNode(dirCapacity);
    root.setPageID(getRootEntry().getEntryID());

    // noinspection PointlessArithmeticExpression
    file.setNextPageID(getRootEntry().getEntryID() + 1);
    FlatRStarTreeNode leaf = createNewLeafNode(leafCapacity);
    file.writePage(leaf);
    HyperBoundingBox mbr = new HyperBoundingBox(new double[exampleLeaf.getDimensionality()], new double[exampleLeaf.getDimensionality()]);
    // noinspection unchecked
    root.addDirectoryEntry(new SpatialDirectoryEntry(leaf.getPageID(), mbr));

    setHeight(2);
  }

  /**
   * Returns true if in the specified node an overflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow occurred, false otherwise
   */
  @Override
  protected boolean hasOverflow(FlatRStarTreeNode node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }
    else if(node.getNumEntries() == node.getCapacity()) {
      node.increaseEntries();
    }
    return false;
  }

  /**
   * Returns true if in the specified node an underflow occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for underflow
   * @return true if in the specified node an underflow occurred, false
   *         otherwise
   */
  @Override
  protected boolean hasUnderflow(FlatRStarTreeNode node) {
    if(node.isLeaf()) {
      return node.getNumEntries() < leafMinimum;
    }
    else {
      return false;
    }
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
  protected SpatialEntry createNewLeafEntry(DBID id) {
    return new SpatialPointLeafEntry(id, relation.get(id));
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(FlatRStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getPageID(), node.getMBR());
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  /**
   * Does nothing.
   */
  @Override
  protected void preInsert(@SuppressWarnings("unused") SpatialEntry entry) {
    // do nothing
  }

  /**
   * Does nothing.
   */
  @SuppressWarnings("unused")
  @Override
  protected void postDelete(DBID id) {
    // do nothing
  }

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<FlatRStarTreeNode> getNodeClass() {
    return FlatRStarTreeNode.class;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}