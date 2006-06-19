package de.lmu.ifi.dbs.index.metrical.mtreevariants.mktab;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeNode;
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
class MkTabTreeNode<O extends DatabaseObject, D extends Distance<D>, N extends MkTabTreeNode<O, D, N, E>, E extends MkTabEntry<D>> extends MTreeNode<O, D, N, E> {
  /**
   * Empty constructor for Externalizable interface.
   */
  public MkTabTreeNode() {
  }

  /**
   * Creates a MkMaxTreeNode object.
   *
   * @param file     the file storing the MkMax-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MkTabTreeNode(PageFile<N> file, int capacity,
                       boolean isLeaf) {
    super(file, capacity, isLeaf);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected N createNewLeafNode(int capacity) {
    return (N) new MkTabTreeNode<O, D, N, E>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected N createNewDirectoryNode(int capacity) {
    return (N) new MkTabTreeNode<O, D,N, E>(getFile(), capacity, false);
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
        E entry = getEntry(i);
        D kDist = result.remove(j);
        result.add(j, Util.max(kDist, entry.getKnnDistance(j + 1)));
      }
    }

    return result;
  }
}
