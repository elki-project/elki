package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Represents a node in a RDkNN-Tree.
 *
 * @author Elke Achtert 
 * @param <D> Distance type
 * @param <N> Number type
 */
public class RdKNNNode<D extends NumberDistance<D,N>, N extends Number> extends AbstractRStarTreeNode<RdKNNNode<D,N>, RdKNNEntry<D,N>> {
  private static final long serialVersionUID = 1;

  /**
   * Empty constructor for Externalizable interface.
   */
  public RdKNNNode() {
	  // empty constructor
  }

  /**
   * Creates a new RdKNNNode object.
   *
   * @param file     the file storing the RdKNN-Tree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow)
   *                 of this node
   * @param isLeaf   indicates whether this node is a leaf node
   */
  public RdKNNNode(PageFile<RdKNNNode<D,N>> file, int capacity, boolean isLeaf) {
    super(file, capacity, isLeaf, RdKNNEntry.class);
  }

  /**
   * Computes and returns the aggregated knn distance of this node
   *
   * @return the aggregated knn distance of this node
   */
  protected D kNNDistance() {
    D result = getEntry(0).getKnnDistance();
    for (int i = 1; i < getNumEntries(); i++) {
      D knnDistance = getEntry(i).getKnnDistance();
      result = DistanceUtil.max(result, knnDistance);
    }
    return result;
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected RdKNNNode<D,N> createNewLeafNode(int capacity) {
    return new RdKNNNode<D,N>(getFile(), capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected RdKNNNode<D,N> createNewDirectoryNode(int capacity) {
    return new RdKNNNode<D,N>(getFile(), capacity, false);
  }

  @Override
  public void adjustEntry(RdKNNEntry<D,N> entry) {
    super.adjustEntry(entry);
    entry.setKnnDistance(kNNDistance());
  }

  /**
   * Tests, if the parameters of the entry representing this node, are correctly set.
   * Subclasses may need to overwrite this method.
   *
   * @param parent the parent holding the entry representing this node
   * @param index  the index of the entry in the parents child array
   */
  @Override
  protected void integrityCheckParameters(RdKNNNode<D,N> parent, int index) {
    super.integrityCheckParameters(parent, index);
    // test if knn distance is correctly set
    RdKNNEntry<D,N> entry = parent.getEntry(index);
    D knnDistance = kNNDistance();
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
