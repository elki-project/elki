package de.lmu.ifi.dbs.index.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.index.spatial.SpatialEntry;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in a flat R*-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FlatRStarTreeNode extends AbstractRStarTreeNode<FlatRStarTreeNode, SpatialEntry> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public FlatRStarTreeNode() {
  }

  /**
   * Creates a new FlatRStarTreeNode with the specified parameters.
   *
   * @param file     the file storing the R*-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public FlatRStarTreeNode(PageFile<FlatRStarTreeNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * @see AbstractRStarTreeNode#createNewLeafNode(int)
   */
  protected FlatRStarTreeNode createNewLeafNode(int capacity) {
    return new FlatRStarTreeNode(getFile(), capacity, true);
  }

  /**
   * @see AbstractRStarTreeNode#createNewDirectoryNode(int)
   */
  protected FlatRStarTreeNode createNewDirectoryNode(int capacity) {
    return new FlatRStarTreeNode(getFile(), capacity, false);
  }
}
