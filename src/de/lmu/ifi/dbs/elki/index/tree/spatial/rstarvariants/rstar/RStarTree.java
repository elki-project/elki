package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialPointLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * RStarTree is a spatial index structure based on the concepts of the R*-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure and ensures persistence.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has RStarTreeNode oneway - - contains
 * 
 * @param <O> Object type
 */
@Title("R*-Tree")
@Description("Balanced index structure based on bounding rectangles.")
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url="http://dx.doi.org/10.1145/93597.98741")
public class RStarTree<O extends NumberVector<O, ?>> extends NonFlatRStarTree<O, RStarTreeNode, SpatialEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RStarTree.class);

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
  public RStarTree(Relation<O> relation, String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(relation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }

  /**
   * Creates an entry representing the root node.
   */
  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewLeafEntry(DBID id) {
    return new SpatialPointLeafEntry(id, relation.get(id));
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(RStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getPageID(), node.getMBR());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected RStarTreeNode createNewLeafNode(int capacity) {
    return new RStarTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected RStarTreeNode createNewDirectoryNode(int capacity) {
    return new RStarTreeNode(file, capacity, false);
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
  protected Class<RStarTreeNode> getNodeClass() {
    return RStarTreeNode.class;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}