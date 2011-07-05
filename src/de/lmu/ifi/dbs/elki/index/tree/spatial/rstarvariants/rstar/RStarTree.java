package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
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
 */
@Title("R*-Tree")
@Description("Balanced index structure based on bounding rectangles.")
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class RStarTree extends NonFlatRStarTree<RStarTreeNode, SpatialEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RStarTree.class);

  /**
   * Constructor.
   * 
   * @param pagefile Page file
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   * @param insertionCandidates insertion candidate set size
   */
  public RStarTree(PageFile<RStarTreeNode> pagefile, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(pagefile, bulk, bulkLoadStrategy, insertionCandidates);
  }

  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(RStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getPageID(), node.computeMBR());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @return a new leaf node
   */
  @Override
  protected RStarTreeNode createNewLeafNode() {
    return new RStarTreeNode(leafCapacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @return a new directory node
   */
  @Override
  protected RStarTreeNode createNewDirectoryNode() {
    return new RStarTreeNode(dirCapacity, false);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}