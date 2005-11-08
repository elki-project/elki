package de.lmu.ifi.dbs.index.metrical.mtree.mknn;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtree.MTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Represents a node in a MkNN-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkNNTreeNode<O extends MetricalObject, D extends Distance> extends MTreeNode<O, D> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkNNTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkNNTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkNNTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkNNTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * Each subclass must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkNNTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkNNTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Determines and returns the knn distance of this node as the maximum
   * knn distance of all entries.
   *
   * @param distanceFunction the distance function
   * @return the knn distance of this node
   */
  protected D kNNDistance(DistanceFunction<O, D> distanceFunction) {
    D knnDist = distanceFunction.nullDistance();
    for (int i = 0; i < numEntries; i++) {
      MkNNEntry<D> entry = (MkNNEntry<D>) entries[i];
      knnDist = Util.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }
}
