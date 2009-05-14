package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Represents a node in a DeLiClu-Tree.
 *
 * @author Elke Achtert 
 */
public class DeLiCluNode extends AbstractRStarTreeNode<DeLiCluNode, DeLiCluEntry> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public DeLiCluNode() {
	  // empty constructor
  }

  /**
   * Creates a new DeLiCluNode with the specified parameters.
   *
   * @param file     the file storing the DeLiClu-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates whether this node is a leaf node
   */
  public DeLiCluNode(PageFile<DeLiCluNode> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf, DeLiCluEntry.class);
  }

  /**
   * Returns true, if the children of this node (or their child nodes)
   * contain handled data objects.
   *
   * @return true, if the children of this node (or their child nodes)
   *         contain handled data objects
   */
  public boolean hasHandled() {
    for (int i = 1; i < getNumEntries(); i++) {
      boolean handled = getEntry(i).hasHandled();
      if (handled) return true;
    }
    return false;
  }

  /**
   * Returns true, if the children of this node (or their child nodes)
   * contain unhandled data objects.
   *
   * @return true, if the children of this node (or their child nodes)
   *         contain unhandled data objects
   */
  public boolean hasUnhandled() {
    for (int i = 1; i < getNumEntries(); i++) {
      boolean handled = getEntry(i).hasUnhandled();
      if (handled) return true;
    }
    return false;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected DeLiCluNode createNewLeafNode(int capacity) {
    return new DeLiCluNode(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected DeLiCluNode createNewDirectoryNode(int capacity) {
    return new DeLiCluNode(getFile(), capacity, false);
  }

  @Override
  public void adjustEntry(DeLiCluEntry entry) {
    super.adjustEntry(entry);
    // adjust hasHandled and hasUnhandled flag
    boolean hasHandled = hasHandled();
    boolean hasUnhandled = hasUnhandled();
    entry.setHasHandled(hasHandled);
    entry.setHasUnhandled(hasUnhandled);
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent the parent holding the entry representing this node
   * @param index  the index of the entry in the parents child array
   */
  @Override
  protected void integrityCheckParameters(DeLiCluNode parent, int index) {
    super.integrityCheckParameters(parent, index);
    // test if hasHandled and hasUnhandled flag are correctly set
    DeLiCluEntry entry = parent.getEntry(index);
    boolean hasHandled = hasHandled();
    boolean hasUnhandled = hasUnhandled();
    if (entry.hasHandled() != hasHandled) {
      String soll = Boolean.toString(hasHandled);
      String ist = Boolean.toString(entry.hasHandled());
      throw new RuntimeException("Wrong hasHandled in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry.getID() + ")" +
                                 "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
    if (entry.hasUnhandled() != hasUnhandled) {
      String soll = Boolean.toString(hasUnhandled);
      String ist = Boolean.toString(entry.hasUnhandled());
      throw new RuntimeException("Wrong hasUnhandled in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry.getID() + ")" +
                                 "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
  }
}
