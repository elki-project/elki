package de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.tree.metrical.mtreevariants.*;

/**
 * MTree is a metrical index structure based on the concepts of the M-Tree.
 * Apart from organizing the objects it also provides several methods to search
 * for certain object in the structure. Persistence is not yet ensured.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTree<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTree<O, D, MTreeNode<O, D>, MTreeEntry<D>> {

  /**
   * Provides a new M-Tree.
   */
  public MTree() {
    super();
    this.debug = true;
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(MTreeEntry<D> entry) {
    // do nothing
  }

  /**
   * @see AbstractMTree#createNewLeafEntry(DatabaseObject, Distance)
   */
  protected MTreeEntry<D> createNewLeafEntry(O object, D parentDistance) {
    return new MTreeLeafEntry<D>(object.getID(), parentDistance);
  }

  /**
   * @see AbstractMTree#createNewDirectoryEntry(AbstractMTreeNode,Integer,Distance)
   */
  protected MTreeEntry<D> createNewDirectoryEntry(MTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
    return new MTreeDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(),
                                      node.coveringRadius(routingObjectID, this));
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected MTreeEntry<D> createRootEntry() {
    return new MTreeDirectoryEntry<D>(null, null, 0, null);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MTreeNode<O, D>(file, capacity, false);
  }
}
