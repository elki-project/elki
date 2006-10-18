package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Represents a node in a MkMax-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkMaxTreeNode<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeNode<O, D, MkMaxTreeNode<O,D>, MkMaxEntry<D>> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkMaxTreeNode() {
  }

  /**
   * Creates a new MkMaxTreeNode object.
   *
   * @param file     the file storing the MkMaxTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkMaxTreeNode(PageFile<MkMaxTreeNode<O,D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity. Each subclass must
   * override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkMaxTreeNode<O,D> createNewLeafNode(int capacity) {
    return new MkMaxTreeNode<O, D>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity. Each subclass
   * must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkMaxTreeNode<O,D> createNewDirectoryNode(int capacity) {
    return new MkMaxTreeNode<O, D>(getFile(), capacity, false);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum knn
   * distance of all entries.
   *
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected D kNNDistance(DistanceFunction<O, D> distanceFunction) {
    D knnDist = distanceFunction.nullDistance();
    for (int i = 0; i < getNumEntries(); i++) {
      MkMaxEntry<D> entry = getEntry(i);
      knnDist = Util.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }

  /**
   * @see AbstractMTreeNode#adjustEntry(de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeEntry, Integer, Distance, de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTree)
   */
  public void adjustEntry(MkMaxEntry<D> entry, Integer routingObjectID, D parentDistance, AbstractMTree<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distance
    entry.setKnnDistance(kNNDistance(mTree.getDistanceFunction()));
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent           the parent holding the entry representing this node
   * @param index            the index of the entry in the parents child arry
   * @param mTree
   */
  protected void test(MkMaxEntry<D> parentEntry, MkMaxTreeNode<O, D> parent, int index, AbstractMTree<O,D,MkMaxTreeNode<O, D>,MkMaxEntry<D>> mTree) {
    super.test(parentEntry, parent, index, mTree);
    // test if knn distance is correctly set
    MkMaxEntry<D> entry = parent.getEntry(index);
    D knnDistance = kNNDistance(mTree.getDistanceFunction());
    if (!entry.getKnnDistance().equals(knnDistance)) {
      String soll = knnDistance.toString();
      String ist = entry.getKnnDistance().toString();
      throw new RuntimeException("Wrong knnDistance in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry + ")" + "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
  }
}
