package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * RStarTree is a spatial index structure based on the concepts of the R*-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure and ensures persistence.
 * 
 * @author Elke Achtert
 * @param <O> Object type
 */
@Title("R*-Tree")
@Description("Balanced index structure based on bounding rectangles.")
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url="http://dx.doi.org/10.1145/93597.98741")
public class RStarTree<O extends NumberVector<O, ?>> extends NonFlatRStarTree<O, RStarTreeNode, SpatialEntry> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RStarTree(Parameterization config) {
    super(config);
    // this.debug = true;
  }

  /**
   * Creates an entry representing the root node.
   */
  @Override
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  @Override
  protected SpatialEntry createNewLeafEntry(O o) {
    return new SpatialLeafEntry(o.getID(), getValues(o));
  }

  @Override
  protected SpatialEntry createNewDirectoryEntry(RStarTreeNode node) {
    return new SpatialDirectoryEntry(node.getID(), node.mbr());
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

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<RStarTreeNode> getNodeClass() {
    return RStarTreeNode.class;
  }
}
