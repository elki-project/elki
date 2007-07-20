package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;

/**
 * Represents a node in an M-Tree.
 *
 * @author Elke Achtert
 */
public class MTreeNode<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeNode<O,D,MTreeNode<O,D>,MTreeEntry<D>> {

  /**
   * Empty constructor for Externalizable interface.
   */
  public MTreeNode() {
	  // empty constructor
  }

   /**
   * Creates a new MTreeNode with the specified parameters.
   *
   * @param file     the file storing the M-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MTreeNode(PageFile<MTreeNode<O,D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Subclasses have to overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MTreeNode<O,D> createNewLeafNode(int capacity) {
    return new MTreeNode<O, D>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Subclasses have to overwrite this method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MTreeNode<O,D> createNewDirectoryNode(int capacity) {
    return new MTreeNode<O, D>(getFile(), capacity, false);
  }
}
