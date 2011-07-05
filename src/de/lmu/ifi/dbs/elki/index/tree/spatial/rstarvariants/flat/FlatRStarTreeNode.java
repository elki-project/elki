package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Represents a node in a flat R*-Tree.
 * 
 * @author Elke Achtert
 */
public class FlatRStarTreeNode extends AbstractRStarTreeNode<FlatRStarTreeNode, SpatialEntry> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public FlatRStarTreeNode() {
    // empty constructor
  }

  /**
   * Deletes the entry at the specified index and shifts all entries after the
   * index to left.
   * 
   * @param index the index at which the entry is to be deleted
   */
  @Override
  public boolean deleteEntry(int index) {
    if(this.getPageID() == 0 && index == 0 && getNumEntries() == 1) {
      return false;
    }
    return super.deleteEntry(index);
  }

  /**
   * Creates a new FlatRStarTreeNode with the specified parameters.
   * 
   * @param capacity the capacity (maximum number of entries plus 1 for
   *        overflow) of this node
   * @param isLeaf indicates whether this node is a leaf node
   */
  public FlatRStarTreeNode(int capacity, boolean isLeaf) {
    super(capacity, isLeaf, SpatialEntry.class);
  }

  /**
   * Increases the length of the entries array to entries.length + 1.
   */
  public final void increaseEntries() {
    SpatialEntry[] tmp = entries;
    entries = ClassGenericsUtil.newArrayOfNull(tmp.length + 1, SpatialEntry.class);
    System.arraycopy(tmp, 0, entries, 0, tmp.length);
  }
}