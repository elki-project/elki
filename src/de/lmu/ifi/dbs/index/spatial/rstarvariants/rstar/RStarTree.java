package de.lmu.ifi.dbs.index.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.NoFlatRStarTree;

/**
 * RStarTree is a spatial index structure based on the concepts of the R*-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure and ensures persistence.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RStarTree <O extends NumberVector> extends NoFlatRStarTree<O, RStarTreeNode, SpatialEntry> {

  /**
   * Creates an entry representing the root node.
   */
  protected SpatialEntry createRootEntry() {
    return new SpatialDirectoryEntry(0, null);
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTree#createNewLeafEntry(NumberVector o)
   */
  protected SpatialEntry createNewLeafEntry(O o) {
    return new SpatialLeafEntry(o.getID(), getValues(o));
  }

  /**
   * @see de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTree#createNewDirectoryEntry(de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTreeNode)
   */
  protected SpatialEntry createNewDirectoryEntry(RStarTreeNode node) {
    //noinspection unchecked
    return new SpatialDirectoryEntry(node.getID(), node.mbr());
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RStarTreeNode createNewLeafNode(int capacity) {
    return new RStarTreeNode(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RStarTreeNode createNewDirectoryNode(int capacity) {
    return new RStarTreeNode(file, capacity, false);
  }


}
