package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkmax;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeNode;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.utilities.Util;

/**
 * Represents a node in a MkMax-Tree.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
class MkMaxTreeNode<O extends DatabaseObject, D extends Distance<D>, N extends MkMaxTreeNode<O, D, N, E>, E extends MkMaxEntry<D>> extends MTreeNode<O, D,N, E> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkMaxTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkMaxTreeNode(PageFile<N> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity. Each subclass must
   * override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected N createNewLeafNode(int capacity) {
    return (N) new MkMaxTreeNode<O, D,N,E>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity. Each subclass
   * must override thois method.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected N createNewDirectoryNode(int capacity) {
    return (N) new MkMaxTreeNode<O, D,N,E>(getFile(), capacity, false);
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
      E entry = getEntry(i);
      knnDist = Util.max(knnDist, entry.getKnnDistance());
    }
    return knnDist;
  }
}
