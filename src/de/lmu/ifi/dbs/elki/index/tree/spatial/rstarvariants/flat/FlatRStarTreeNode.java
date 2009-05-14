package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Represents a node in a flat R*-Tree.
 *
 * @author Elke Achtert 
 */
public class FlatRStarTreeNode extends AbstractRStarTreeNode<FlatRStarTreeNode, SpatialEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public FlatRStarTreeNode() {
	  // empty constructor
  }

  /**
   * Deletes the entry at the specified index and shifts all
   * entries after the index to left.
   *
   * @param index the index at which the entry is to be deleted
   */
  @Override
  public boolean deleteEntry(int index) {
    if (this.getID() == 0 && index == 0 && getNumEntries()==1) return false;
    return super.deleteEntry(index);
  }

  /**
   * Creates a new FlatRStarTreeNode with the specified parameters.
   *
   * @param file     the file storing the R*-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates whether this node is a leaf node
   */
  public FlatRStarTreeNode(PageFile<FlatRStarTreeNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf, SpatialEntry.class);
  }

  @Override
  protected FlatRStarTreeNode createNewLeafNode(int capacity) {
    return new FlatRStarTreeNode(getFile(), capacity, true);
  }

  @Override
  protected FlatRStarTreeNode createNewDirectoryNode(int capacity) {
    return new FlatRStarTreeNode(getFile(), capacity, false);
  }
}
