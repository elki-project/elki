package de.lmu.ifi.dbs.index.metrical.mtreevariants.mktab;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a MkMax-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkTabTreeNode<O extends DatabaseObject, D extends Distance<D>> extends AbstractMTreeNode<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkTabTreeNode() {
  }

  /**
   * Creates a MkTabTreeNode object.
   *
   * @param file     the file storing the MkTab-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkTabTreeNode(PageFile<MkTabTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkTabTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkTabTreeNode<O, D>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkTabTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkTabTreeNode<O, D>(getFile(), capacity, false);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum knn
   * distance of all entries.
   *
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected List<D> kNNDistances(DistanceFunction<O, D> distanceFunction) {
    int k = getEntry(0).getK_max();

    List<D> result = new ArrayList<D>();
    for (int i = 0; i < k; i++) {
      result.add(distanceFunction.nullDistance());
    }

    for (int i = 0; i < getNumEntries(); i++) {
      for (int j = 0; j < k; j++) {
        MkTabEntry<D> entry = getEntry(i);
        D kDist = result.remove(j);
        result.add(j, Util.max(kDist, entry.getKnnDistance(j + 1)));
      }
    }

    return result;
  }

  /**
   * @see AbstractMTreeNode#adjustEntry(de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeEntry, Integer, Distance, de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTree)
   */
  public void adjustEntry(MkTabEntry<D> entry, Integer routingObjectID, D parentDistance, AbstractMTree<O, D, MkTabTreeNode<O, D>, MkTabEntry<D>> mTree) {
    super.adjustEntry(entry, routingObjectID, parentDistance, mTree);
    // adjust knn distances
    entry.setKnnDistances(kNNDistances(mTree.getDistanceFunction()));
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent           the parent holding the entry representing this node
   * @param index            the index of the entry in the parents child arry
   * @param mTree
   */
  protected void test(MkTabEntry<D> parentEntry, MkTabTreeNode<O, D> parent, int index, AbstractMTree<O,D,MkTabTreeNode<O, D>,MkTabEntry<D>> mTree) {
    super.test(parentEntry, parent, index, mTree);
    // test knn distances
    MkTabEntry<D> entry = parent.getEntry(index);
    List<D> knnDistances = kNNDistances(mTree.getDistanceFunction());
    if (!entry.getKnnDistances().equals(knnDistances)) {
      String soll = knnDistances.toString();
      String ist = entry.getKnnDistances().toString();
      throw new RuntimeException("Wrong knnDistances in node "
                                 + parent.getID() + " at index " + index + " (child "
                                 + entry.getID() + ")" + "\nsoll: " + soll
                                 + ",\n ist: " + ist);
    }
  }
}
