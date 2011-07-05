package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;

/**
 * Represents a node in an R*-Tree.
 * 
 * @author Elke Achtert
 */
public class RStarTreeNode extends AbstractRStarTreeNode<RStarTreeNode, SpatialEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RStarTreeNode() {
    // empty constructor
  }

  /**
   * Creates a new RStarTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public RStarTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf, SpatialEntry.class);
  }
}